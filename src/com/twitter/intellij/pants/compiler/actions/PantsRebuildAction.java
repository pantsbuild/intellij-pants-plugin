// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * PantsRebuildAction is a UI action that, when in a project, runs clean-all, then compiles all targets in the project
 */
public class PantsRebuildAction extends PantsCompileAllTargetsAction {

  @Override
  public void update(AnActionEvent e) {
    e.getPresentation().setText("Compile all targets with clean-all");
  }

  @Override
  public boolean doCleanAll() {
    return true;
  }
}
