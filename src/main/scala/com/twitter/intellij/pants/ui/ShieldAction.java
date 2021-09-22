// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Shield action allows previous action's text to still be displayed
 * and update, but disables all possible user interaction
 */
public class ShieldAction extends AnAction {
  private AnAction shieldedAction;

  public ShieldAction(@Nullable AnAction action) {
    shieldedAction = action;
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
  }

  @Override
  public void update(AnActionEvent event) {
    if (shieldedAction != null) {
      shieldedAction.update(event);
    }
    event.getPresentation().setEnabled(false);
  }
}