// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.task;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.task.AbstractExternalSystemTaskManager;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PantsTaskManager extends AbstractExternalSystemTaskManager<PantsExecutionSettings>
  implements ExternalSystemTaskManager<PantsExecutionSettings> {
  @Override
  public void executeTasks(
    @NotNull ExternalSystemTaskId id,
    @NotNull List<String> taskNames,
    @NotNull String projectPath,
    @Nullable PantsExecutionSettings settings,
    @NotNull List<String> vmOptions,
    @NotNull List<String> scriptParameters,
    @Nullable String debuggerSetup,
    @NotNull ExternalSystemTaskNotificationListener listener
  ) throws ExternalSystemException {

  }

  @Override
  public boolean cancelTask(@NotNull ExternalSystemTaskId id, @NotNull ExternalSystemTaskNotificationListener listener)
    throws ExternalSystemException {
    return false;
  }
}
