package com.twitter.intellij.pants.psi.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.PyArgumentList;
import com.jetbrains.python.psi.PyCallExpression;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import org.jetbrains.annotations.NotNull;

public class PantsTargetPathReferenceProvider extends PsiReferenceProvider {
  @NotNull
  @Override
  public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    final PsiElement parent = element.getParent();
    if (!(parent instanceof PyArgumentList)) {
      return PsiReference.EMPTY_ARRAY;
    }
    final PsiElement parentParent = parent.getParent();
    if (!(parentParent instanceof PyCallExpression)) {
      return PsiReference.EMPTY_ARRAY;
    }
    else if ((((PyCallExpression)parentParent).isCalleeText("pants"))) {
      return getReferences((PyStringLiteralExpression)element);
    }
    return PsiReference.EMPTY_ARRAY;
  }

  @NotNull
  private PsiReference[] getReferences(PyStringLiteralExpression expression) {
    return new PantsTargetReferenceSet(expression).getAllReferences();
  }
}
