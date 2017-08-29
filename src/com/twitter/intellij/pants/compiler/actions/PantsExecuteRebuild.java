// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

public class PantsExecuteRebuild implements IPantsExecuteTask {
  @NotNull
  public PantsMakeBeforeRun.PantsExecuteTaskResult apply(@NotNull PantsMakeBeforeRun runner, @NotNull Project project, @NotNull Set<String> targetAddresses) {
    return runner.executeCompileTask(project, targetAddresses, Arrays.asList("clean-all", "export-classpath", "compile"));
  }
}
