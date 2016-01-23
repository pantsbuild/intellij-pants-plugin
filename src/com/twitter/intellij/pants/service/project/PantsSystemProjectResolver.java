// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.ide.FileSelectInContext;
import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.Consumer;
import com.twitter.intellij.pants.projectview.PantsProjectPaneSelectInTarget;
import com.twitter.intellij.pants.projectview.ProjectFilesViewPane;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
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
    // Non-blocking function
    if (id.findProject() != null) {
      switchToProjectFilesTreeView(id.findProject(), projectPath);
    }
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
    final DataNode<ProjectData> projectDataNode = new DataNode<ProjectData>(ProjectKeys.PROJECT, projectData, null);

    if (!isPreviewMode) {
      resolveUsingPantsGoal(id, executor, listener, projectDataNode);

      if (!containsContentRoot(projectDataNode, executor.getProjectDir())) {
        // Add a module with content root as import project directory path.
        // This will allow all the files in the imported project directory will be indexed by the plugin.
        final String moduleName = executor.getRootModuleName();
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
        if (FileUtil.filesEqual(executor.getWorkingDir(), new File(executor.getProjectPath()))) {
          contentRoot.storePath(ExternalSystemSourceType.EXCLUDED, executor.getWorkingDir().getPath() + "/.idea");
        }
        moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);
      }
    }

    return projectDataNode;
  }

  private boolean containsContentRoot(@NotNull DataNode<ProjectData> projectDataNode, @NotNull String path) {
    for (DataNode<ModuleData> moduleDataNode : ExternalSystemApiUtil.findAll(projectDataNode, ProjectKeys.MODULE)) {
      for (DataNode<ContentRootData> contentRootDataNode : ExternalSystemApiUtil.findAll(moduleDataNode, ProjectKeys.CONTENT_ROOT)) {
        final ContentRootData contentRootData = contentRootDataNode.getData();
        if (FileUtil.isAncestor(contentRootData.getRootPath(), path, false)) {
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

  private void switchToProjectFilesTreeView(final Project project, final String projectPath) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (ProjectView.getInstance(project).getPaneIds().contains(ProjectFilesViewPane.ID)) {
          ProjectView.getInstance(project).changeView(ProjectFilesViewPane.ID);
          focusOnImportedDirectory(project, projectPath);
          return;
        }
        else {
          // Wait then launch another non-blocking thread to check GUI ready before exiting this UI thread.
          try {
            Thread.sleep(1000);
          }
          catch (InterruptedException e) {
          }
          switchToProjectFilesTreeView(project, projectPath);
        }
      }
    });
  }

  private void focusOnImportedDirectory(final Project project, final String projectPath) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (ModuleManager.getInstance(project).getModules().length > 0) {
          final VirtualFile target = VirtualFileManager.getInstance().findFileByUrl("file://" + projectPath);
          SelectInContext selectInContext = new FileSelectInContext(project, target);
          for (SelectInTarget selectInTarget : ProjectView.getInstance(project).getSelectInTargets()) {
            if (selectInTarget instanceof PantsProjectPaneSelectInTarget) {
              selectInTarget.selectIn(selectInContext, false);
              break;
            }
          }
          return;
        }
        else {
          try {
            Thread.sleep(1000);
          }
          catch (InterruptedException e) {
          }
          focusOnImportedDirectory(project, projectPath);
        }
      }
    });
  }
}
