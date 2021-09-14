// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.reference;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    Optional<VirtualFile> virtualFile = findFile();
    if (!virtualFile.isPresent()) {
      return null;
    }
    VirtualFile buildFileOrDirectory = PantsUtil.findBUILDFiles(virtualFile.get())
      .stream()
      .findFirst()
      .orElse(virtualFile.get());

    final PsiManager psiManager = PsiManager.getInstance(getElement().getProject());
    final PsiFile buildFile = psiManager.findFile(buildFileOrDirectory);
    return buildFile != null ? buildFile : psiManager.findDirectory(buildFileOrDirectory);
  }
}
