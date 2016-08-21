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
import com.jetbrains.python.psi.PyCallExpression;
import com.twitter.intellij.pants.util.PantsPsiUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PantsTargetReference extends PantsPsiReferenceBase {
  public PantsTargetReference(@NotNull PsiElement element, @NotNull TextRange range, @Nls String text, @Nls String relativePath) {
    super(element, range, text, relativePath);
  }

  @NotNull
  private Collection<PsiFile> findBuildFiles() {
    if (StringUtil.isEmpty(getRelativePath())) {
      // same file reference
      return Collections.singleton(getElement().getContainingFile());
    }

    Optional<VirtualFile> file = findFile();
    if (!file.isPresent()) {
      return Collections.emptyList();
    }
    Collection<VirtualFile> buildFiles = PantsUtil.findBUILDFiles(file.get());
    final PsiManager psiManager = PsiManager.getInstance(getElement().getProject());
    return buildFiles.stream().map(psiManager::findFile).collect(Collectors.toSet());
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    return findBuildFiles().stream()
      .map(PantsPsiUtil::findTargets)
      .map(Map::keySet)
      .flatMap(Collection::stream)
      .map(LookupElementBuilder::create)
      .toArray();
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    Collection<PsiFile> files = findBuildFiles();
    for(PsiFile file: files) {
      Map<String, PyCallExpression> targets = PantsPsiUtil.findTargets(file);
      if (targets.containsKey(getText())) {
        return targets.get(getText());
      }
    }

    return files.stream().findFirst().orElse(null);
  }
}
