// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.reference;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PathUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PantsVirtualFileReference extends PantsPsiReferenceBase {
  public PantsVirtualFileReference(@NotNull PsiElement element, @NotNull TextRange range, @Nls String text, @Nls String relativePath) {
    super(element, range, text, relativePath);
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    final PsiManager psiManager = PsiManager.getInstance(getElement().getProject());
    final Optional<VirtualFile> parent = findFile(PathUtil.getParentPath(getRelativePath()));
    List<LookupElementBuilder> variants =
      parent.map(
        file -> Arrays.stream(file.getChildren())
          .filter(VirtualFile::isDirectory)
          .map(f -> {
            final PsiFile psiFile = psiManager.findFile(f);
            return psiFile == null ? LookupElementBuilder.create(f.getPresentableName()) : LookupElementBuilder.create(psiFile);
          })
          .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
    return ArrayUtil.toObjectArray(variants);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    Stream<VirtualFile> buildFileOrDirectoryCandidates = findFile()
      .map(file -> PantsUtil.findBUILDFiles(file).stream())
      .orElse(Stream.empty());
    PsiManager psiManager = PsiManager.getInstance(getElement().getProject());
    Stream<PsiElement> resolvedCandidates = buildFileOrDirectoryCandidates.flatMap(candidate -> {
      Optional<PsiFile> file = Optional.ofNullable(psiManager.findFile(candidate));
      Optional<PsiDirectory> directory = Optional.ofNullable(psiManager.findDirectory(candidate));
      Optional<PsiElement> toAdd = PantsUtil.join(file, directory);
      return toAdd.map(Stream::of).orElse(Stream.empty());
    });
    return resolvedCandidates.findFirst().orElse(null);
  }
}
