// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.python.psi.PyQualifiedExpression;
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider;
import com.jetbrains.python.psi.resolve.RatedResolveResult;
import com.twitter.intellij.pants.index.PantsTargetIndex;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PantsReferenceResolveProvider implements PyReferenceResolveProvider {
  @NotNull
  @Override
  public List<RatedResolveResult> resolveName(@NotNull PyQualifiedExpression element) {
    PsiFile containingFile = element.getContainingFile();
    return PantsUtil.isBUILDFileName(containingFile.getName()) ?
           resolvePantsName(element) :
           Collections.<RatedResolveResult>emptyList();
  }

  private List<RatedResolveResult> resolvePantsName(@NotNull PyQualifiedExpression element) {
    final String name = element.getName();
    return ContainerUtil.map(
      PantsTargetIndex.resolveTargetByName(name, element.getProject()),
      new Function<PsiElement, RatedResolveResult>() {
        @Override
        public RatedResolveResult fun(PsiElement element) {
          return new RatedResolveResult(RatedResolveResult.RATE_NORMAL, element);
        }
      }
    );
  }
}