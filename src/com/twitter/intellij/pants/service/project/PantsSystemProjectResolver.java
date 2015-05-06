// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class PantsSystemProjectResolver implements ExternalSystemProjectResolver<PantsExecutionSettings> {
  protected static final Logger LOG = Logger.getInstance(PantsSystemProjectResolver.class);

  private final Map<ExternalSystemTaskId, PantsCompileOptionsExecutor> task2executor =
    new ConcurrentHashMap<ExternalSystemTaskId, PantsCompileOptionsExecutor>();

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
    final PantsCompileOptionsExecutor executor = PantsCompileOptionsExecutor.create(projectPath, settings, !isPreviewMode);
    task2executor.put(id, executor);
    final DataNode<ProjectData> projectDataNode = resolveProjectInfoImpl(id, executor, listener, isPreviewMode);
    task2executor.remove(id);
    return projectDataNode;
  }

  @NotNull
  private DataNode<ProjectData> resolveProjectInfoImpl(
    @NotNull ExternalSystemTaskId id,
    @NotNull final PantsCompileOptionsExecutor executor,
    @NotNull ExternalSystemTaskNotificationListener listener,
    boolean isPreviewMode
  ) throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
    // todo(fkorotkov): add ability to choose a name for a project
    final ProjectData projectData = new ProjectData(
      PantsConstants.SYSTEM_ID,
      executor.getProjectName(),
      executor.getWorkingDir().getPath() + "/.idea/pants-projects/" + executor.getProjectRelativePath(),
      executor.getProjectPath()
    );
    final DataNode<ProjectData> projectDataNode = new DataNode<ProjectData> (ProjectKeys.PROJECT, projectData, null);

    if (!isPreviewMode || ApplicationManager.getApplication().isUnitTestMode()) {
      try {
        resolveUsingPantsGoal(id, executor, listener, projectDataNode);
      }
      catch (ExternalSystemException e) {
        final Project project = ContainerUtil.find(
          ProjectManager.getInstance().getOpenProjects(),
          new Condition<Project>() {
            @Override
            public boolean value(Project project) {
              return StringUtil.equals(project.getName(), executor.getProjectName());
            }
          }
        );
        if (project == null) {
          LOG.error(e);
        } else {
          final ExternalSystemNotificationManager notificationManager = ExternalSystemNotificationManager.getInstance(project);
          notificationManager.processExternalProjectRefreshError(e, project.getName(), PantsConstants.SYSTEM_ID);
        }
      }
    }

    if (!containsContentRoot(projectDataNode, executor.getProjectDir())) {
      // Add a module with content root as import project directory path.
      // This will allow all the files in the imported project directory will be indexed by the plugin.
      final String moduleName = PantsUtil.getCanonicalModuleName(executor.getProjectRelativePath());
      final ModuleData moduleData = new ModuleData(
        PantsConstants.PANTS_PROJECT_MODULE_ID_PREFIX + moduleName,
        PantsConstants.SYSTEM_ID,
        ModuleTypeId.JAVA_MODULE,
        moduleName + PantsConstants.PANTS_PROJECT_MODULE_SUFFIX,
        projectData.getIdeProjectFileDirectoryPath() + "/" + moduleName,
        executor.getProjectPath()
      );
      final DataNode<ModuleData> moduleDataNode = projectDataNode.createChild(ProjectKeys.MODULE, moduleData);
      final ContentRootData contentRoot = new ContentRootData(PantsConstants.SYSTEM_ID, executor.getProjectDir());
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
    @NotNull PantsCompileOptionsExecutor executor,
    final ExternalSystemTaskNotificationListener listener,
    @NotNull DataNode<ProjectData> projectDataNode
  ) {
    final PantsResolver dependenciesResolver = new PantsResolver(executor);
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
    final PantsCompileOptionsExecutor executor = task2executor.remove(taskId);
    return executor != null && executor.cancelAllProcesses();
  }
}
