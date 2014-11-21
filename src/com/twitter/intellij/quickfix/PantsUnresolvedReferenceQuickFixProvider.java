// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.quickfix;

import com.intellij.codeInsight.daemon.QuickFixActionRegistrar;
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PantsUnresolvedReferenceQuickFixProvider extends UnresolvedReferenceQuickFixProvider {
  @NotNull
  @Override
  public Class getReferenceClass() {
    return PsiJavaCodeReferenceElement.class;
  }

  @Override
  public void registerFixes(@NotNull PsiReference reference, @NotNull QuickFixActionRegistrar registrar) {
    final PsiElement unresolvedPsiElement = reference.getElement();
    @NonNls final String referenceName = reference.getRangeInElement().substring(unresolvedPsiElement.getText());

    Project project = unresolvedPsiElement.getProject();
    PsiFile containingFile = unresolvedPsiElement.getContainingFile();
    if (containingFile == null) return;

    final VirtualFile containingClassFile = containingFile.getVirtualFile();
    if (containingClassFile == null) return;

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final Module containingModule = fileIndex.getModuleForFile(containingClassFile);
    final PantsTargetAddress currentAddress = containingModule != null ? PantsUtil.getTargetAddressFromModule(containingModule) : null;
    if (currentAddress == null) return;
    final PsiClass[] classes = PsiShortNamesCache.getInstance(project).getClassesByName(referenceName, GlobalSearchScope.allScope(project));
    final List<PsiClass> allowedDependencies = filterAllowedDependencies(unresolvedPsiElement, classes);
    for (PsiClass dependency : allowedDependencies) {
      final Module module = ModuleUtil.findModuleForPsiElement(dependency);
      final PantsTargetAddress addressToAdd = module != null ? PantsUtil.getTargetAddressFromModule(module) : null;
      if (module != null && addressToAdd != null) {
        registrar.register(new AddPantsTargetDependencyFix(currentAddress, addressToAdd));
      }
      // todo(fkoroktov): handle jars
    }
  }

  private List<PsiClass> filterAllowedDependencies(PsiElement element, PsiClass[] classes) {
    final DependencyValidationManager dependencyValidationManager = DependencyValidationManager.getInstance(element.getProject());
    final PsiFile fromFile = element.getContainingFile();
    final List<PsiClass> result = new ArrayList<PsiClass>();
    for (PsiClass psiClass : classes) {
      if (dependencyValidationManager.getViolatorDependencyRule(fromFile, psiClass.getContainingFile()) == null) {
        result.add(psiClass);
      }
    }
    return result;
  }
}
