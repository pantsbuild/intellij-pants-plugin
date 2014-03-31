package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsProjectResolver implements ExternalSystemProjectResolver<PantsExecutionSettings> {
  @Nullable
  @Override
  public DataNode<ProjectData> resolveProjectInfo(
    @NotNull ExternalSystemTaskId id,
    @NotNull String projectPath,
    boolean isPreviewMode,
    @Nullable PantsExecutionSettings settings,
    @NotNull ExternalSystemTaskNotificationListener listener
  ) throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
    final String projectDirPath = PathUtil.getParentPath(projectPath);
    final ProjectData projectData = new ProjectData(
      PantsConstants.SYSTEM_ID,
      StringUtil.notNullize(settings != null ? settings.getTargetName() : null, "project"),
      projectDirPath,
      projectPath
    );
    return new DataNode<ProjectData>(ProjectKeys.PROJECT, projectData, null);
  }

  @Override
  public boolean cancelTask(@NotNull ExternalSystemTaskId taskId, @NotNull ExternalSystemTaskNotificationListener listener) {
    return false;
  }
}
