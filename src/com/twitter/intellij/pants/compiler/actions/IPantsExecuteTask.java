// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;

import java.util.Set;

public interface IPantsExecuteTask {
  @NotNull
  PantsMakeBeforeRun.PantsExecuteTaskResult apply(
    @NotNull PantsMakeBeforeRun runner, @NotNull Project project, @NotNull Set<String> targetAddresses);
}
