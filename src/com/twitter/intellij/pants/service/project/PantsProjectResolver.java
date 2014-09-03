package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
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
    final DataNode<ProjectData> projectDataNode = getProjectDataNode(projectPath, settings);

    resolveUsingNewAPI(id, projectPath, settings, listener, projectDataNode, isPreviewMode);

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
    final PantsResolver dependenciesResolver = new PantsResolver(projectPath, settings, isPreviewMode);

    listener.onStatusChange(new ExternalSystemTaskNotificationEvent(id, "Resolving dependencies..."));
    dependenciesResolver.resolve(id, listener);
    dependenciesResolver.addInfo(projectDataNode);
  }

  private DataNode<ProjectData> getProjectDataNode(String projectPath, PantsExecutionSettings settings) {
    final String projectDirPath = settings.isAllTargets() ? projectPath : PathUtil.getParentPath(projectPath);
    // todo(fkorotkov): add ability to choose a name for a project
    final String targetsSuffix = settings.isAllTargets() ? ":" : StringUtil.join(settings.getTargetNames(), " :");
    final String projectName = PathUtil.getFileName(projectDirPath) + "/:" + targetsSuffix;
    final ProjectData projectData = new ProjectData(
      PantsConstants.SYSTEM_ID,
      projectName,
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
