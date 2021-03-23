// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.ProjectTopics;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.ide.FileSelectInContext;
import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.ProjectSdkData;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.service.project.ExternalSystemProjectResolver;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBusConnection;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListenerManager;
import com.twitter.intellij.pants.projectview.PantsProjectPaneSelectInTarget;
import com.twitter.intellij.pants.projectview.ProjectFilesViewPane;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsSdkUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PantsSystemProjectResolver implements ExternalSystemProjectResolver<PantsExecutionSettings> {
  protected static final Logger LOG = Logger.getInstance(PantsSystemProjectResolver.class);

  private final Map<ExternalSystemTaskId, PantsCompileOptionsExecutor> task2executor =
    new ConcurrentHashMap<>();

  private static void clearPantsModules(@NotNull Project project, String projectPath, DataNode<ProjectData> projectDataNode) {
    Runnable clearModules = () -> {
      Set<String> importedModules = projectDataNode.getChildren().stream()
        .map(node -> node.getData(ProjectKeys.MODULE))
        .filter(Objects::nonNull)
        .map(ModuleData::getInternalName)
        .collect(Collectors.toSet());

      Module[] modules = ModuleManager.getInstance(project).getModules();
      for (Module module : modules) {
        boolean hasPantsProjectPath = Objects.equals(module.getOptionValue(PantsConstants.PANTS_OPTION_LINKED_PROJECT_PATH), Paths.get(projectPath).normalize().toString());
        boolean isNotBeingImported = !importedModules.contains(module.getName());
        if (hasPantsProjectPath && isNotBeingImported) {
          ModuleManager.getInstance(project).disposeModule(module);
        }
      }
    };

    Application application = ApplicationManager.getApplication();
    application.invokeAndWait(() -> application.runWriteAction(clearModules));
  }

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

    final DataNode<ProjectData> projectDataNode =
      resolveProjectInfoImpl(id, executor, listener, settings, isPreviewMode);

    // We do not want to repeatedly force switching to 'Project Files Tree' view if
    // user decides to use import dep as jar and wants to use the more focused 'Project' view.
    if (!settings.isImportSourceDepsAsJars()) {
      doViewSwitch(id, projectPath);
    }
    task2executor.remove(id);
    // Removing the existing modules right before returning to minimize the time user observes
    // that the old modules are gone.
    Optional.ofNullable(id.findProject()).ifPresent(p -> clearPantsModules(p, projectPath, projectDataNode));
    return projectDataNode;
  }

  private void doViewSwitch(@NotNull ExternalSystemTaskId id, @NotNull String projectPath) {
    Project ideProject = id.findProject();
    if (ideProject == null) {
      return;
    }
    // Disable zooming on subsequent project resolves/refreshes,
    // i.e. a project that already has existing modules, because it may zoom at a module
    // that is going to be replaced by the current resolve.
    if (ModuleManager.getInstance(ideProject).getModules().length > 0) {
      return;
    }

    MessageBusConnection messageBusConnection = ideProject.getMessageBus().connect();
    messageBusConnection.subscribe(
      ProjectTopics.PROJECT_ROOTS,
      new ModuleRootListener() {
        @Override
        public void rootsChanged(ModuleRootEvent event) {
          // Initiate view switch only when project modules have been created.
          new ViewSwitchProcessor(ideProject, projectPath).asyncViewSwitch();
        }
      }
    );
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
    final Optional<VirtualFile> existingPantsExe = PantsUtil.findPantsExecutable(projectFilePath);
    final Optional<VirtualFile> newPantsExe = PantsUtil.findPantsExecutable(projectPath);
    if (!existingPantsExe.isPresent() || !newPantsExe.isPresent()) {
      return;
    }
    if (!existingPantsExe.get().getCanonicalFile().getPath().equals(newPantsExe.get().getCanonicalFile().getPath())) {
      throw new ExternalSystemException(String.format(
        "Failed to import. Target/Directory to be added uses a different pants executable %s compared to the existing project's %s",
        existingPantsExe, newPantsExe
      ));
    }
  }

  @NotNull
  private DataNode<ProjectData> resolveProjectInfoImpl(
    @NotNull ExternalSystemTaskId id,
    @NotNull final PantsCompileOptionsExecutor executor,
    @NotNull ExternalSystemTaskNotificationListener listener,
    @NotNull PantsExecutionSettings settings,
    boolean isPreviewMode
  ) throws ExternalSystemException, IllegalArgumentException, IllegalStateException {
    String projectName = settings.getProjectName().orElseGet(executor::getDefaultProjectName);
    Path projectPath = Paths.get(
      executor.getBuildRoot().getPath(),
      ".idea",
      "pants-projects",
      // Use a timestamp hash to avoid module creation dead lock
      // when overlapping targets were imported into multiple projects
      // from the same Pants repo.
      DigestUtils.sha1Hex(Long.toString(System.currentTimeMillis())),
      executor.getProjectRelativePath()
    );

    final ProjectData projectData = new ProjectData(
      PantsConstants.SYSTEM_ID,
      projectName,
      projectPath.toString(),
      executor.getProjectPath()
    );
    final DataNode<ProjectData> projectDataNode = new DataNode<>(ProjectKeys.PROJECT, projectData, null);

    PantsUtil.findPantsExecutable(executor.getProjectPath())
      .flatMap(file -> PantsSdkUtil.getDefaultJavaSdk(file.getPath(), null))
      .map(sdk -> new ProjectSdkData(sdk.getName()))
      .ifPresent(sdk -> projectDataNode.createChild(ProjectSdkData.KEY, sdk));

    if (!isPreviewMode) {
      PantsExternalMetricsListenerManager.getInstance().logIsIncrementalImport(settings.incrementalImportDepth().isPresent());
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
        if (FileUtil.filesEqual(executor.getBuildRoot(), new File(executor.getProjectPath()))) {
          contentRoot.storePath(ExternalSystemSourceType.EXCLUDED, executor.getBuildRoot().getPath() + "/.idea");
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
    @NotNull final ExternalSystemTaskId id,
    @NotNull PantsCompileOptionsExecutor executor,
    final ExternalSystemTaskNotificationListener listener,
    @NotNull DataNode<ProjectData> projectDataNode
  ) {
    final PantsResolver dependenciesResolver = new PantsResolver(executor);
    ProcessAdapter processAdapter = new ProcessAdapter() {
      @Override
      public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        listener.onTaskOutput(id, event.getText(), outputType == ProcessOutputTypes.STDOUT);
      }
    };
    dependenciesResolver.resolve(
      status -> listener.onStatusChange(new ExternalSystemTaskNotificationEvent(id, status)),
      processAdapter
    );
    dependenciesResolver.addInfoTo(projectDataNode);
  }

  @Override
  public boolean cancelTask(@NotNull ExternalSystemTaskId taskId, @NotNull ExternalSystemTaskNotificationListener listener) {
    final PantsCompileOptionsExecutor executor = task2executor.remove(taskId);
    return executor != null && executor.cancelAllProcesses();
  }

  private static class ViewSwitchProcessor {
    private final Project myProject;
    private final String myProjectPath;
    private ScheduledFuture<?> directoryFocusHandle;

    public ViewSwitchProcessor(final Project project, final String projectPath) {
      myProject = project;
      myProjectPath = projectPath;
    }

    public void asyncViewSwitch() {
      /**
       * Make sure the project view opened so the view switch will follow.
       */
      final ToolWindow projectWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.PROJECT_VIEW);
      if (projectWindow == null) {
        return;
      }
      ApplicationManager.getApplication().invokeLater(() -> {
        // Show Project Pane, and switch to ProjectFilesViewPane right after.
        projectWindow.show(() -> {
          ProjectView.getInstance(myProject).changeView(ProjectFilesViewPane.ID);
          // Disable directory focus as it may cause too much stress when
          // there is heavy indexing load right after project import.
          // https://youtrack.jetbrains.com/issue/IDEA-204959
          // queueFocusOnImportDirectory();
        });
      });
    }

    private void queueFocusOnImportDirectory() {
      directoryFocusHandle = PantsUtil.scheduledThreadPool.scheduleWithFixedDelay(() -> {
        if (ModuleManager.getInstance(myProject).getModules().length == 0 ||
            !ProjectView.getInstance(myProject).getCurrentViewId().equals(ProjectFilesViewPane.ID)) {
          return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
          final VirtualFile pathImported = LocalFileSystem.getInstance().findFileByPath(myProjectPath);
          // Skip focusing if directory is not found.
          if (pathImported != null) {
            VirtualFile importDirectory = pathImported.isDirectory() ? pathImported : pathImported.getParent();
            SelectInContext selectInContext = new FileSelectInContext(myProject, importDirectory);
            for (SelectInTarget selectInTarget : ProjectView.getInstance(myProject).getSelectInTargets()) {
              if (selectInTarget instanceof PantsProjectPaneSelectInTarget) {
                selectInTarget.selectIn(selectInContext, false);
                break;
              }
            }
          }
          final boolean mayInterruptIfRunning = true;
          directoryFocusHandle.cancel(mayInterruptIfRunning);
        });
      }, 0, 1, TimeUnit.SECONDS);
    }
  }
}
