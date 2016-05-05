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
import com.intellij.openapi.projectRoots.Sdk;
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
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    checkForDifferentPantsExecutables(id, projectPath);

    final PantsCompileOptionsExecutor executor = PantsCompileOptionsExecutor.create(projectPath, settings);
    task2executor.put(id, executor);
    final DataNode<ProjectData> projectDataNode = resolveProjectInfoImpl(id, executor, listener, isPreviewMode);
    doViewSwitch(id, projectPath);
    task2executor.remove(id);
    return projectDataNode;
  }

  private void doViewSwitch(@NotNull ExternalSystemTaskId id, @NotNull String projectPath) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }
    Project ideProject = id.findProject();
    if (ideProject == null) {
      return;
    }
    ViewSwitchProcessor vsp  = new ViewSwitchProcessor(ideProject, projectPath);
    vsp.asyncViewSwitch();
  }

  /**
   * Check whether the pants executable of the new project to import is the same as the existing project's pants executable.
   */
  private void checkForDifferentPantsExecutables(@NotNull ExternalSystemTaskId id, @NotNull String projectPath) {
    final Project existingIdeProject = id.findProject();
    if (existingIdeProject == null) {
      return;
    }
    String projectFilePath = existingIdeProject.getProjectFilePath();
    if (projectFilePath == null) {
      return;
    }
    final VirtualFile existingPantsExe = PantsUtil.findPantsExecutable(projectFilePath);
    if (existingPantsExe == null) {
      return;
    }
    final VirtualFile newPantExe = PantsUtil.findPantsExecutable(projectPath);
    if (!existingPantsExe.getCanonicalFile().getPath().equals(newPantExe.getCanonicalFile().getPath())) {
      throw new ExternalSystemException(String.format(
        "Failed to import. Target/Directory to be added uses a different pants executable %s compared to the existing project's %s",
        existingPantsExe, newPantExe
      ));
    }
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

    //VirtualFile pantsExecutable = PantsUtil.findPantsExecutable(executor.getProjectPath());
    //if (pantsExecutable != null) {
    //  Sdk sdk = PantsUtil.getDefaultJavaSdk(pantsExecutable.getPath());
    //  if (sdk != null) {
    //    projectDataNode.createChild(PantsConstants.SDK_KEY, sdk);
    //  }
    //}

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

  private class ViewSwitchProcessor {
    private final Project myProject;
    private final String myProjectPath;
    private ScheduledFuture<?> viewSwitchHandle;
    private ScheduledFuture<?> directoryFocusHandle;

    public ViewSwitchProcessor(final Project project, final String projectPath) {
      myProject = project;
      myProjectPath = projectPath;
    }

    public void asyncViewSwitch() {
      queueSwitchToProjectFilesTreeView();
    }

    private void queueSwitchToProjectFilesTreeView() {
      viewSwitchHandle = PantsUtil.scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
        @Override
        public void run() {
          if (!ProjectView.getInstance(myProject).getPaneIds().contains(ProjectFilesViewPane.ID)) {
            return;
          }
          ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
              ProjectView.getInstance(myProject).changeView(ProjectFilesViewPane.ID);
              queueFocusOnImportDirectory();
              viewSwitchHandle.cancel(false);
            }
          });
        }
      }, 0, 1, TimeUnit.SECONDS);
    }

    private void queueFocusOnImportDirectory() {
      directoryFocusHandle = PantsUtil.scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
        @Override
        public void run() {
          if (ModuleManager.getInstance(myProject).getModules().length == 0 ||
              !ProjectView.getInstance(myProject).getCurrentViewId().equals(ProjectFilesViewPane.ID)) {
            return;
          }
          ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
              final VirtualFile importDirectory = VirtualFileManager.getInstance().findFileByUrl("file://" + myProjectPath);
              // Skip focusing if directory is not found.
              if (importDirectory != null) {
                SelectInContext selectInContext = new FileSelectInContext(myProject, importDirectory);
                for (SelectInTarget selectInTarget : ProjectView.getInstance(myProject).getSelectInTargets()) {
                  if (selectInTarget instanceof PantsProjectPaneSelectInTarget) {
                    selectInTarget.selectIn(selectInContext, false);
                    break;
                  }
                }
              }
              directoryFocusHandle.cancel(false);
            }
          });
        }
      }, 0, 1, TimeUnit.SECONDS);
    }
  }
}
