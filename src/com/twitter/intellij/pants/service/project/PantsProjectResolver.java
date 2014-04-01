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
    final DataNode<ModuleData> moduleNode = createModuleNode(projectPath, projectDataNode);

    final PantsSourceRootsResolver sourceRootsResolver = new PantsSourceRootsResolver(projectPath, settings);

    listener.onStatusChange(new ExternalSystemTaskNotificationEvent(id, "Resolving source roots..."));
    sourceRootsResolver.resolve(id, listener);
    sourceRootsResolver.addInfo(moduleNode);

    return projectDataNode;
  }

  private DataNode<ModuleData> createModuleNode(String projectPath, DataNode<ProjectData> projectDataNode) {
    final String name = "test";
    final ModuleData moduleData = new ModuleData(
      name,
      PantsConstants.SYSTEM_ID,
      ModuleTypeId.JAVA_MODULE,
      name,
      PathUtil.getParentPath(projectPath),
      projectPath
    );
    return projectDataNode.createChild(ProjectKeys.MODULE, moduleData);
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
