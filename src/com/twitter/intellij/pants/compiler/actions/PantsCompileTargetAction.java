// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import org.jetbrains.annotations.NotNull;

/**
 * PantsCompileTargetAction is a UI action that is used to compile a Pants target or collection of targets
 */
public class PantsCompileTargetAction extends PantsTaskActionBase {

  public PantsCompileTargetAction(@NotNull String targetAddress) {
    super(new PantsGetGivenTargets(targetAddress),
          new PantsExecuteCompile(),
          targetAddress);
  }
}
