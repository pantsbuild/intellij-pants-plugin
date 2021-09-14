// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;

import com.intellij.codeInsight.daemon.QuickFixActionRegistrar;
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider;
import com.intellij.openapi.application.ApplicationManager;
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
    // FIXME: Re-enable quick fix for missing dependencies once it is functional again.
    // https://github.com/pantsbuild/intellij-pants-plugin/issues/280
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }
    for (PantsQuickFix quickFix : PantsUnresolvedReferenceFixFinder.findMissingDependencies(reference)) {
      registrar.register(quickFix);
    }
  }
}
