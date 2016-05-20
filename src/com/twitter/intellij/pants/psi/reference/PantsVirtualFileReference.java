// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.reference;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.ObjectUtils;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PantsVirtualFileReference extends PantsPsiReferenceBase {
  public PantsVirtualFileReference(@NotNull PsiElement element, @NotNull TextRange range, @Nls String text, @Nls String relativePath) {
    super(element, range, text, relativePath);
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    final PsiManager psiManager = PsiManager.getInstance(getElement().getProject());
    final VirtualFile parent = findFile(PathUtil.getParentPath(getRelativePath()));
    final List<Object> variants = ContainerUtil.map(
      ContainerUtil.filter(
        parent != null ? parent.getChildren() : VirtualFile.EMPTY_ARRAY,
        new Condition<VirtualFile>() {
          @Override
          public boolean value(VirtualFile file) {
            return file.isDirectory();
          }
        }
      ),
      new Function<VirtualFile, Object>() {
        @Override
        public Object fun(VirtualFile file) {
          final PsiFile psiFile = psiManager.findFile(file);
          if (psiFile != null) {
            return LookupElementBuilder.create(psiFile);
          }
          else {
            return LookupElementBuilder.create(file.getPresentableName());
          }
        }
      }
    );
    return ArrayUtil.toObjectArray(variants);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    VirtualFile virtualFile = findFile();
    if (virtualFile == null) {
      return null;
    }
    VirtualFile buildFileOrDirectory = ObjectUtils.notNull(PantsUtil.findBUILDFile(virtualFile), virtualFile);

    final PsiManager psiManager = PsiManager.getInstance(getElement().getProject());
    final PsiFile buildFile = psiManager.findFile(buildFileOrDirectory);
    return buildFile != null ? buildFile : psiManager.findDirectory(buildFileOrDirectory);
  }
}
