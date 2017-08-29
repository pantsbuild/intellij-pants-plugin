// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PantsExecuteLint implements IPantsExecuteTask {
  @NotNull
  public PantsMakeBeforeRun.PantsExecuteTaskResult apply(@NotNull PantsMakeBeforeRun runner, @NotNull Project project, @NotNull Set<String> targetAddresses) {
    List<String> args = new ArrayList<>();
    args.add("lint");
    args.addAll(targetAddresses);
    return runner.executeTask(project, args);
  }
}
