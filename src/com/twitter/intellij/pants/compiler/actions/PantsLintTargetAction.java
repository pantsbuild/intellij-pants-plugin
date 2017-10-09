// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsExecuteTaskResult;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * PantsLintTargetAction is a UI action that lints target(s) related to the file under edit.
 */
public class PantsLintTargetAction extends PantsTaskActionBase {

  private final Optional<Module> module;

  public PantsLintTargetAction(Optional<Module> module) {
    super("Lint target(s) in the selected editor");
    this.module = module;
  }

  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    Optional<Module> module = this.module;
    if (!module.isPresent()) {
      module = PantsUtil.getFileForEvent(e)
        .flatMap(file -> PantsUtil.getModuleForFile(file, project));
    }
    return module
      .map(PantsUtil::getNonGenTargetAddresses)
      .orElse(new LinkedList<>())
      .stream();
  }

  @NotNull
  @Override
  public PantsExecuteTaskResult execute(@NotNull PantsMakeBeforeRun runner,
                                        @NotNull Project project,
                                        @NotNull Set<String> targetAddresses) {
    return runner.executeTask(
      project, Sets.newHashSet("lint"), targetAddresses);
  }
}
