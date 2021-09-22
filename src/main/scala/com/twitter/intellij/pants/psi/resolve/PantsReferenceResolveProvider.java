// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.resolve;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.python.psi.PyQualifiedExpression;
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider;
import com.jetbrains.python.psi.resolve.RatedResolveResult;
import com.jetbrains.python.psi.types.TypeEvalContext;
import com.twitter.intellij.pants.index.PantsTargetIndex;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PantsReferenceResolveProvider implements PyReferenceResolveProvider {

  @NotNull
  @Override
  public List<RatedResolveResult> resolveName(@NotNull PyQualifiedExpression expression, @NotNull TypeEvalContext context) {
    PsiFile containingFile = expression.getContainingFile();
    if (isOneOfAvailableTargetTypes(expression)) {
      List<RatedResolveResult> resolved = new LinkedList<>();
      resolved.add(new RatedResolveResult(RatedResolveResult.RATE_NORMAL, expression));
      return resolved;
    }
    else {
      return PantsUtil.isBUILDFileName(containingFile.getName()) ?
             resolvePantsName(expression) :
             Collections.<RatedResolveResult>emptyList();
    }
  }

  private boolean isOneOfAvailableTargetTypes(@NotNull PyQualifiedExpression expression) {
    String[] allBuildTypes = PropertiesComponent.getInstance().getValues(PantsConstants.PANTS_AVAILABLE_TARGETS_KEY);
    return allBuildTypes != null && Arrays.asList(allBuildTypes).contains(expression.getReferencedName());
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