// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.module.Module;

/**
 * PantsCompileAllTargetsInModuleAction is a UI action that is used to compile all Pants targets for a module
 */
public class PantsCompileAllTargetsInModuleAction extends PantsTaskActionBase {

  public PantsCompileAllTargetsInModuleAction() {
    this(null);
  }

  public PantsCompileAllTargetsInModuleAction(Module module) {
    super(new PantsGetModuleTargets(module),
          new PantsExecuteCompile(),
          "Compile all targets in module");
  }
}
