// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

/**
 * PantsLintCurrentTargetAction is a UI action that lints target(s) related to the file under edit.
 */
public class PantsLintCurrentTargetAction extends PantsTaskActionBase {

  public PantsLintCurrentTargetAction() {
    super(new PantsGetCurrentFileTargets(),
          new PantsExecuteLint(),
          "Lint target(s) in the selected editor");
  }
}
