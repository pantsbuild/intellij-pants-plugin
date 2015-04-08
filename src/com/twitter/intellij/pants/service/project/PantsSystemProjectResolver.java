// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.PathUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;


public class PantsSystemProjectResolver implements ExternalSystemProjectResolver<PantsExecutionSettings> {
  @Nullable
  @Override
  public DataNode<ProjectData> resolveProjectInfo(
    @NotNull ExternalSystemTaskId id,
    @NotNull String projectPath,
    boolean isPreviewMode,
    @Nullable PantsExecutionSettings settings,
    @NotNull ExternalSystemTaskNotificationListener listener
  ) throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
    if (projectPath.startsWith(".pants.d")) {
      return null;
    }
    final int targetNameDelimiterIndex = projectPath.indexOf(':');
    if (targetNameDelimiterIndex > 0) {
      // normalizing. we don't have per module settings and a linked project path of a module contains target name.
      projectPath = projectPath.substring(0, targetNameDelimiterIndex);
    }
    boolean allTargets = settings == null || settings.isAllTargets();
    final String projectDirPath = allTargets ? projectPath : PathUtil.getParentPath(projectPath);
    final VirtualFile workingDir = PantsUtil.findPantsWorkingDir(projectDirPath);
    if (workingDir == null) {
      throw new ExternalSystemException(PantsBundle.message("pants.error.no.pants.executable.by.path", projectDirPath));
    }
    // todo(fkorotkov): add ability to choose a name for a project
    final String targetsSuffix = allTargets ? ":" : StringUtil.join(settings.getTargetNames(), " :");
    final String relativeProjectPath = PantsUtil.getRelativeProjectPath(new File(workingDir.getPath()), projectDirPath);
    final String projectName = relativeProjectPath + "/:" + targetsSuffix;
    final ProjectData projectData = new ProjectData(
      PantsConstants.SYSTEM_ID,
      projectName,
      workingDir.getPath() + "/.idea/pants-projects/" + relativeProjectPath,
      projectPath
    );
    final DataNode<ProjectData> projectDataNode = new DataNode<ProjectData> (ProjectKeys.PROJECT, projectData, null);

    resolveUsingPantsGoal(id, projectPath, settings, listener, projectDataNode, isPreviewMode);

    if (!containsContentRoot(projectDataNode, projectDirPath)) {
      // Add a module with content root as import project directory path.
      // This will allow all the files in the imported project directory will be indexed by the plugin.
      final String moduleName = PantsUtil.getCanonicalModuleName(relativeProjectPath);
      final ModuleData moduleData = new ModuleData(
        PantsConstants.PANTS_PROJECT_MODULE_ID_PREFIX + moduleName,
        PantsConstants.SYSTEM_ID,
        ModuleTypeId.JAVA_MODULE,
        moduleName + PantsConstants.PANTS_PROJECT_MODULE_SUFFIX,
        projectData.getIdeProjectFileDirectoryPath() + "/" + moduleName,
        projectDirPath
      );
      final DataNode<ModuleData> moduleDataNode = projectDataNode.createChild(ProjectKeys.MODULE, moduleData);
      final ContentRootData contentRoot = new ContentRootData(PantsConstants.SYSTEM_ID, projectDirPath);
      moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);
    }

    return projectDataNode;
  }

  private boolean containsContentRoot(@NotNull DataNode<ProjectData> projectDataNode, @NotNull String path) {
    for (DataNode<ModuleData> moduleDataNode : ExternalSystemApiUtil.findAll(projectDataNode, ProjectKeys.MODULE)) {
      for (DataNode<ContentRootData> contentRootDataNode : ExternalSystemApiUtil.findAll(moduleDataNode, ProjectKeys.CONTENT_ROOT)) {
        final ContentRootData contentRootData = contentRootDataNode.getData();
        if (StringUtil.equalsIgnoreCase(path, contentRootData.getRootPath())) {
          return true;
        }
      }
    }

    return false;
  }

  private void resolveUsingPantsGoal(
    final ExternalSystemTaskId id,
    String projectPath,
    PantsExecutionSettings settings,
    final ExternalSystemTaskNotificationListener listener,
    DataNode<ProjectData> projectDataNode,
    boolean isPreviewMode
  ) {
    final PantsResolver dependenciesResolver = new PantsResolver(projectPath, settings, isPreviewMode);
    dependenciesResolver.resolve(
      new Consumer<String>() {
        @Override
        public void consume(String status) {
          listener.onStatusChange(new ExternalSystemTaskNotificationEvent(id, status));
        }
      },
      new ProcessAdapter() {
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          listener.onTaskOutput(id, event.getText(), outputType == ProcessOutputTypes.STDOUT);
        }
      }
    );
    dependenciesResolver.addInfoTo(projectDataNode);
  }

  @Override
  public boolean cancelTask(@NotNull ExternalSystemTaskId taskId, @NotNull ExternalSystemTaskNotificationListener listener) {
    return false;
  }
}
