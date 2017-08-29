// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

/**
 * PantsCompileCurrentTargetAction is a UI action that compiles target(s) related to the file under edit.
 */
public class PantsCompileCurrentTargetAction extends PantsTaskActionBase {

  public PantsCompileCurrentTargetAction() {
    super(new PantsGetCurrentFileTargets(),
          new PantsExecuteCompile(),
          "Compile target(s) in the selected editor");
  }
}
