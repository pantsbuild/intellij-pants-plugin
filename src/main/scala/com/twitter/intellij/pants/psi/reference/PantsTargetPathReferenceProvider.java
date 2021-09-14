// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.PyKeywordArgument;
import com.jetbrains.python.psi.PyListLiteralExpression;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PantsTargetPathReferenceProvider extends PsiReferenceProvider {
  @NotNull
  @Override
  public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    final PyStringLiteralExpression stringLiteral = PsiTreeUtil.getParentOfType(element, PyStringLiteralExpression.class, false);
    final PsiElement parent = stringLiteral != null ? stringLiteral.getParent() : null;
    if (!(parent instanceof PyListLiteralExpression)) {
      return PsiReference.EMPTY_ARRAY;
    }
    final PsiElement parentParent = parent.getParent();
    if (parentParent instanceof PyKeywordArgument &&
        "dependencies".equalsIgnoreCase(((PyKeywordArgument)parentParent).getKeyword())) {
      return getReferences(stringLiteral);
    }
    return PsiReference.EMPTY_ARRAY;
  }

  @NotNull
  private PsiReference[] getReferences(PyStringLiteralExpression expression) {
    final List<PsiReference> references = new PantsTargetReferenceSet(expression).getReferences();
    return ArrayUtil.toObjectArray(references, PsiReference.class);
  }
}
