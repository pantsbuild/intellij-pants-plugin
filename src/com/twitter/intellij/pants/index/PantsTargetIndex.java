// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.index;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Convertor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.twitter.intellij.pants.util.PantsPsiUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PantsTargetIndex extends ScalarIndexExtension<String> {
  public static final ID<String, Void> NAME = ID.create("PantsTargetIndex");

  public static Collection<String> getTargets(@NotNull Project project) {
    return FileBasedIndex.getInstance().getAllKeys(NAME, project);
  }

  public static List<PsiElement> resolveTargetByName(@Nls String name, @NotNull Project project) {
    return resolveTargetByName(name, project, GlobalSearchScope.allScope(project));
  }

  public static List<PsiElement> resolveTargetByName(@Nls String name, @NotNull Project project, GlobalSearchScope scope) {
    final PsiManager psiManager = PsiManager.getInstance(project);
    final Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(NAME, name, scope);
    final Stream<PsiElement> resolvedTargets = containingFiles.stream()
      .flatMap(virtualFile -> {
        final Optional<PsiFile> foundPsiFile = Optional.ofNullable(psiManager.findFile(virtualFile));
        final Optional<PyFile> foundPyPsi = foundPsiFile
          .flatMap(psiFile -> psiFile instanceof PyFile ? Optional.of((PyFile) psiFile) : Optional.empty());
        final Optional<PyReferenceExpression> referenceExpression = foundPyPsi
          .flatMap(pyPsi -> Optional.ofNullable(PantsPsiUtil.findTargetDefinitions(pyPsi).get(name)));
        final Optional<PsiPolyVariantReference> reference = referenceExpression.map(expr -> expr.getReference());
        final Optional<PsiElement> definition = reference.map(ref -> ref.resolve());
        final Optional<PsiElement> toAdd = PantsUtil.join(definition, referenceExpression);
        return toAdd.map(Stream::of).orElse(Stream.empty());
      });
    return resolvedTargets.collect(Collectors.toList());
  }

  @NotNull
  @Override
  public ID<String, Void> getName() {
    return NAME;
  }

  @NotNull
  @Override
  public DataIndexer<String, Void, FileContent> getIndexer() {
    return myIndexer;
  }

  @NotNull
  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return new EnumeratorStringDescriptor();
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return new DefaultFileTypeSpecificInputFilter(PythonFileType.INSTANCE);
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  private static DataIndexer<String, Void, FileContent> myIndexer = new DataIndexer<String, Void, FileContent>() {
    @Override
    @NotNull
    public Map<String, Void> map(@NotNull final FileContent inputData) {
      final PsiFile psiFile = inputData.getPsiFile();
      if (psiFile instanceof PyFile) {
        final Map<String, PyReferenceExpression> targetDefinitions = PantsPsiUtil.findTargetDefinitions((PyFile)psiFile);
        return ContainerUtil.newMapFromKeys(
          targetDefinitions.keySet().iterator(),
          new Convertor<String, Void>() {
            @Override
            public Void convert(String o) {
              return null;
            }
          }
        );
      }
      return Collections.emptyMap();
    }
  };
}
