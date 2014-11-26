// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;

import com.intellij.codeInsight.daemon.QuickFixActionRegistrar;
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;

public class PantsUnresolvedJavaReferenceQuickFixProvider extends UnresolvedReferenceQuickFixProvider {
  @NotNull
  @Override
  public Class getReferenceClass() {
    return PsiJavaCodeReferenceElement.class;
  }

  @Override
  public void registerFixes(@NotNull PsiReference reference, @NotNull QuickFixActionRegistrar registrar) {
    for (PantsQuickFix quickFix : PantsUnresolvedReferenceFixFinder.findMissingDependencies(reference)) {
      registrar.register(quickFix);
    }
  }
}
