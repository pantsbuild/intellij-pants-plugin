// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.reference;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.util.PantsPsiUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PantsTargetReference extends PantsPsiReferenceBase {
  public PantsTargetReference(@NotNull PsiElement element, @NotNull TextRange range, @Nls String text, @Nls String relativePath) {
    super(element, range, text, relativePath);
  }

  @Nullable
  private PsiFile findBuildFile() {
    if (StringUtil.isEmpty(getRelativePath())) {
      // same file reference
      return getElement().getContainingFile();
    }
    final VirtualFile buildFile = PantsUtil.findBUILDFile(findFile());
    if (buildFile == null) {
      return null;
    }
    final PsiManager psiManager = PsiManager.getInstance(getElement().getProject());
    return psiManager.findFile(buildFile);
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    return ContainerUtil.map2Array(
      PantsPsiUtil.findTargets(findBuildFile()).keySet(),
      new Function<String, Object>() {
        @Override
        public Object fun(String targetName) {
          return LookupElementBuilder.create(targetName);
        }
      }
    );
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    PsiFile file = findBuildFile();
    PsiElement target = PantsPsiUtil.findTargets(file).get(getText());
    // Return the file element instead if target is not found for any reason.
    if (target == null) {
      return file;
    }
    else {
      return target;
    }
  }
}
