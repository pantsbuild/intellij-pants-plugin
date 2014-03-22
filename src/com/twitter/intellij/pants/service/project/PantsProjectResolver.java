package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsProjectResolver implements ExternalSystemProjectResolver<PantsExecutionSettings> {
  @Nullable
  @Override
  public DataNode<ProjectData> resolveProjectInfo(@NotNull ExternalSystemTaskId id, @NotNull String projectPath, boolean isPreviewMode, @Nullable PantsExecutionSettings settings, @NotNull ExternalSystemTaskNotificationListener listener) throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
    return null;
  }

  @Override
  public boolean cancelTask(@NotNull ExternalSystemTaskId taskId, @NotNull ExternalSystemTaskNotificationListener listener) {
    return false;
  }
}
