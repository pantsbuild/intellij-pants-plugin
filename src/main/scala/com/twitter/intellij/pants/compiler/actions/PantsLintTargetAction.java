// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsExecuteTaskResult;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

/**
 * PantsLintTargetAction is a UI action that lints target(s) related to the file under edit.
 */
public class PantsLintTargetAction extends PantsTaskActionBase {

  private final Collection<String> targets;

  public PantsLintTargetAction(@NotNull Collection<String> targets) {
    super("Lint selected target(s)");
    this.targets = targets;
  }

  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    return targets.stream();
  }

  @NotNull
  @Override
  public PantsExecuteTaskResult execute(@NotNull PantsMakeBeforeRun runner,
                                        @NotNull Project project,
                                        @NotNull Set<String> targetAddresses) {
    return runner.invokePants(
      project, targetAddresses, Lists.newArrayList("lint"), "Lint");
  }
}
