package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
    final DataNode<ProjectData> projectDataNode = getProjectDataNode(projectPath, settings);

    resolveUsingNeemovwAPI(id, projectPath, settings, listener, projectDataNode, isPreviewMode);

    return projectDataNode;
  }

  private void resolveUsingNewAPI(
    ExternalSystemTaskId id,
    String projectPath,
    PantsExecutionSettings settings,
    ExternalSystemTaskNotificationListener listener,
    DataNode<ProjectData> projectDataNode,
    boolean isPreviewMode
  ) {
    final PantsDependenciesGraphResolver dependenciesResolver = new PantsDependenciesGraphResolver(projectPath, settings, isPreviewMode);

    listener.onStatusChange(new ExternalSystemTaskNotificationEvent(id, "Resolving dependencies..."));
    dependenciesResolver.resolve(id, listener);
    dependenciesResolver.addInfo(projectDataNode);
  }

  private DataNode<ProjectData> getProjectDataNode(String projectPath, PantsExecutionSettings settings) {
    final String projectDirPath = PathUtil.getParentPath(projectPath);
    final List<String> targetNames = settings != null ? settings.getTargetNames() : null;
    final ProjectData projectData = new ProjectData(
      PantsConstants.SYSTEM_ID,
      StringUtil.notNullize(targetNames != null ? StringUtil.join(targetNames, " ") : null, "project"),
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
