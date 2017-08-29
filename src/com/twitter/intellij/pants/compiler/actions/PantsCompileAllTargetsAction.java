// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

/**
 * PantsCompileAllTargetsAction is a UI action that, when in a project, compiles all targets in the project
 */
public class PantsCompileAllTargetsAction extends PantsTaskActionBase {

  public PantsCompileAllTargetsAction() {
    super(new PantsGetAllTargets(),
          new PantsExecuteCompile(),
          "Compile all targets in project");
  }
}
