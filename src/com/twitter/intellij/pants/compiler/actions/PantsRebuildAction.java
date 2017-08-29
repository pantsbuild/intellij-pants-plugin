// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

/**
 * PantsRebuildAction is a UI action that, when in a project, runs clean-all, then compiles all targets in the project
 */
public class PantsRebuildAction extends PantsTaskActionBase {

  public PantsRebuildAction() {
    super(new PantsGetAllTargets(),
          new PantsExecuteRebuild(),
          "Compile all targets with clean-all");
  }
}
