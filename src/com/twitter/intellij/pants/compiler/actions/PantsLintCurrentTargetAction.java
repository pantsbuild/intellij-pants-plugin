// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.ModuleUtil;
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
 * PantsLintCurrentTargetAction is a UI action that lints target(s) related to the file under edit.
 */
public class PantsLintCurrentTargetAction extends PantsTaskActionBase {

  public PantsLintCurrentTargetAction() {
    super("Lint target(s) in the selected editor");
  }

  // FIXME: get smallest list of target addresses including file, NOT just all the addresses in the module!
  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    return PantsUtil.getFileInSelectedEditor(project)
      .flatMap(file -> Optional.ofNullable(ModuleUtil.findModuleForFile(file, project)))
      .map(PantsUtil::getNonGenTargetAddresses)
      .orElse(new LinkedList<>())
      .stream();
  }

  @NotNull
  @Override
  public PantsExecuteTaskResult execute(@NotNull PantsMakeBeforeRun runner,
                                        @NotNull Project project,
                                        @NotNull Set<String> targetAddresses) {
    List<String> args = Lists.newArrayList();
    args.add("lint");
    args.addAll(targetAddresses);
    return runner.executeTask(project, args);
  }
}
