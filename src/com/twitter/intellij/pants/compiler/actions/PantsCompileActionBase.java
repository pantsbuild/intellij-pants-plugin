// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.execution.PantsExecuteTaskResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * PantsCompileActionBase is an abstract action that implements basic compilation activities.
 */
public abstract class PantsCompileActionBase extends PantsTaskActionBase {

  public PantsCompileActionBase(final String description) {
    super(description);
  }

  @NotNull
  @Override
  public PantsExecuteTaskResult execute(@NotNull PantsMakeBeforeRun runner,
                                        @NotNull Project project,
                                        @NotNull Set<String> targetAddresses) {
    return runner.executeCompileTask(project, targetAddresses, false);
  }
}
