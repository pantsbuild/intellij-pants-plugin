// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.twitter.intellij.pants.PantsBundle;
import org.jetbrains.annotations.NotNull;

public abstract class PantsQuickFix implements IntentionAction, LocalQuickFix {
  @NotNull
  @Override
  public String getFamilyName() {
    return PantsBundle.message("quick.fix.group.name");
  }
}
