package com.twitter.intellij.pants.psi.reference;

import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.jetbrains.python.psi.PyStringLiteralExpression;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class PantsReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
      psiElement(PyStringLiteralExpression.class),
      new PantsTargetPathReferenceProvider()
    );
  }
}
