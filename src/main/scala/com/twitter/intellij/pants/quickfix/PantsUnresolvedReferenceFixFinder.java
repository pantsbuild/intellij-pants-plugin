// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PantsUnresolvedReferenceFixFinder {
  @NotNull
  public static List<PantsQuickFix> findMissingDependencies(@NotNull PsiReference reference) {
    final PsiElement unresolvedPsiElement = reference.getElement();
    @NonNls final String referenceName = reference.getRangeInElement().substring(unresolvedPsiElement.getText());

    final PsiFile containingFile = unresolvedPsiElement.getContainingFile();
    return containingFile != null ? findMissingDependencies(referenceName, containingFile) : Collections.<PantsQuickFix>emptyList();
  }

  @NotNull
  public static List<PantsQuickFix> findMissingDependencies(@NotNull String referenceName, @NotNull PsiFile containingFile) {
    final VirtualFile containingClassFile = containingFile.getVirtualFile();
    if (containingClassFile == null) return Collections.emptyList();

    final Project project = containingFile.getProject();

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final Module containingModule = fileIndex.getModuleForFile(containingClassFile);

    final List<PantsTargetAddress> addresses = PantsUtil.getTargetAddressesFromModule(containingModule);
    if (addresses.size() != 1) return Collections.emptyList();

    final PantsTargetAddress currentAddress = addresses.iterator().next();

    final PsiClass[] classes = PsiShortNamesCache.getInstance(project).getClassesByName(referenceName, GlobalSearchScope.allScope(project));
    final List<PsiClass> allowedDependencies = filterAllowedDependencies(containingFile, classes);
    final List<PantsQuickFix> result = new ArrayList<>();
    for (PsiClass dependency : allowedDependencies) {
      final Module module = ModuleUtil.findModuleForPsiElement(dependency);
      for (PantsTargetAddress addressToAdd : PantsUtil.getTargetAddressesFromModule(module)) {
        result.add(new AddPantsTargetDependencyFix(currentAddress, addressToAdd));
      }
      // todo(fkoroktov): handle jars
    }
    return result;
  }

  private static List<PsiClass> filterAllowedDependencies(PsiFile fromFile, PsiClass[] classes) {
    final DependencyValidationManager dependencyValidationManager = DependencyValidationManager.getInstance(fromFile.getProject());
    final List<PsiClass> result = new ArrayList<>();
    for (PsiClass psiClass : classes) {
      if (dependencyValidationManager.getViolatorDependencyRule(fromFile, psiClass.getContainingFile()) == null) {
        result.add(psiClass);
      }
    }
    return result;
  }
}
