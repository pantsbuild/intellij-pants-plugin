// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsExecuteTaskResult;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

/**
 * PantsRebuildAction is a UI action that, when in a project, runs clean-all, then compiles all targets in the project
 */
public class PantsRebuildAction extends PantsTaskActionBase {

  public PantsRebuildAction() {
    super("Compile all targets in project, after running clean-all");
  }

  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    Module[] modules = ModuleManager.getInstance(project).getModules();
    return Arrays.stream(modules)
      .map(PantsUtil::getNonGenTargetAddresses)
      .flatMap(Collection::stream);
  }

  @NotNull
  @Override
  public PantsExecuteTaskResult execute(@NotNull PantsMakeBeforeRun runner,
                                        @NotNull Project project,
                                        @NotNull Set<String> targetAddresses) {
    return runner.executeCompileTask(project, targetAddresses, true);
  }
}
