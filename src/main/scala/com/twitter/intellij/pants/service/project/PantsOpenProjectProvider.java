// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.CommonBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.JavaUiBundle;
import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.importing.OpenProjectProvider;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.FrameInfo;
import com.intellij.platform.PlatformProjectOpenProcessor;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.projectImport.ProjectOpenProcessor;
import com.intellij.projectImport.ProjectOpenedCallback;
import com.twitter.intellij.pants.service.project.wizard.PantsProjectImportProvider;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import static com.intellij.ide.impl.ProjectUtil.focusProjectWindow;
import static com.intellij.ide.impl.ProjectUtil.isSameProject;

final class PantsOpenProjectProvider implements OpenProjectProvider {
  private static PantsOpenProjectProvider instance = new PantsOpenProjectProvider();

  static OpenProjectProvider getInstance() {
    return instance;
  }

  @Override
  public boolean canOpenProject(@NotNull VirtualFile file) {
    if (!PantsUtil.findBuildRoot(file).isPresent()) return false;

    return file.isDirectory() || PantsUtil.isBUILDFileName(file.getName());
  }

  @Override
  public void linkToExistingProject(@NotNull VirtualFile projectFile, @NotNull Project project) {
    AddModuleWizard dialog = openNewProjectWizard(projectFile);
    link(projectFile, project, dialog);
  }

  @Nullable
  @Override
  public Project openProject(@NotNull VirtualFile projectFile, @Nullable Project projectToClose, boolean forceOpenInNewFrame) {
    if (isAlreadyOpen(projectFile)) return null;

    switch (shouldOpenExistingProject(projectFile, projectToClose)) {
      case Messages.NO:
        Project project = importPantsProject(projectFile);
        OpenProjectTask task = openTask(projectToClose, project, forceOpenInNewFrame);
        return ProjectManagerEx.getInstanceEx().openProject(Paths.get(projectFile.getPath()), task);
      case Messages.YES:
        return PlatformProjectOpenProcessor.getInstance().doOpenProject(projectFile, projectToClose, forceOpenInNewFrame);
      default:
        return null;
    }
  }

  public static OpenProjectTask openTask(Project projectToClose, Project project, boolean forceOpenInNewFrame) {
    return new OpenProjectTask(
      forceOpenInNewFrame,
      projectToClose,
      true, // isNewProject
      false, // useDefaultProjectAsTemplate
      project,
      project.getName(), // projectName
      false, // showWelcomeScreen
      null, // callback
      null, //frameManager
      -1, // line
      -1, // column
      false, // isRefreshVfsNeeded
      false, // runConfigurators
      false, // runConversionBeforeOpen
      null, // projectWorkspaceId
      false, //isProjectCreatedWithWizard
      false, // preloadServices
      null, // beforeInit
      null, // beforeOpen
      null // preparedToOpen
    );
  }

  private boolean isAlreadyOpen(VirtualFile projectFile) {
    String projectDirectory = projectDir(projectFile).toString();
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      if (isSameProject(projectDirectory, project)) {
        focusProjectWindow(project, false);
        return true;
      }
    }
    return false;
  }

  private int shouldOpenExistingProject(VirtualFile file, Project projectToClose) {
    VirtualFile rootFile = PantsUtil.findBuildRoot(file).orElse(null);
    if(rootFile == null) return Messages.NO;

    ProjectOpenProcessor openProcessor = PlatformProjectOpenProcessor.getInstance();
    if (!openProcessor.canOpenProject(rootFile)) return Messages.NO;

    VirtualFile dotIdeaFile = rootFile.findChild(Project.DIRECTORY_STORE_FOLDER);
    if (dotIdeaFile == null || !dotIdeaFile.exists()) return Messages.NO;

    Application application = ApplicationManager.getApplication();
    if (application.isHeadlessEnvironment()) return Messages.YES;

    return Messages.showYesNoCancelDialog(
      projectToClose,
      JavaUiBundle.message("project.import.open.existing", "an existing project", rootFile.getPath(), file.getName()),
      IdeBundle.message("title.open.project"),
      JavaUiBundle.message("project.import.open.existing.openExisting"),
      JavaUiBundle.message("project.import.open.existing.reimport"),
      CommonBundle.getCancelButtonText(),
      Messages.getQuestionIcon()
    );
  }

  @Nullable
  private Project importPantsProject(@NotNull VirtualFile projectFile) {
    AddModuleWizard dialog = openNewProjectWizard(projectFile);
    if (dialog == null) return null;

    Project project = createProject(projectFile, dialog);
    if (project == null) return null;

    link(projectFile, project, dialog);
    refresh(projectFile, project);

    return project;
  }

  private void link(@NotNull VirtualFile projectFile, @NotNull Project project, AddModuleWizard dialog) {
    if (dialog == null) return;

    ProjectBuilder builder = dialog.getBuilder(project);
    if (builder == null) return;

    try {
      ApplicationManager.getApplication().runWriteAction(() -> {
        Optional.ofNullable(dialog.getNewProjectJdk())
          .ifPresent(jdk -> NewProjectUtil.applyJdkToProject(project, jdk));

        URI output = projectDir(projectFile).resolve(".out").toUri();
        Optional.ofNullable(CompilerProjectExtension.getInstance(project))
          .ifPresent(ext -> ext.setCompilerOutputUrl(output.toString()));
      });

      builder.commit(project, null, ModulesProvider.EMPTY_MODULES_PROVIDER);
      project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, Boolean.TRUE);

      project.save();
    }
    finally {
      builder.cleanup();
    }
  }

  private void refresh(VirtualFile file, Project project) {
    ExternalSystemUtil.refreshProject(
      file.getPath(),
      new ImportSpecBuilder(project, PantsConstants.SYSTEM_ID).usePreviewMode().use(ProgressExecutionMode.MODAL_SYNC)
    );
    ExternalSystemUtil.refreshProject(
      file.getPath(),
      new ImportSpecBuilder(project, PantsConstants.SYSTEM_ID)
    );
  }

  private AddModuleWizard openNewProjectWizard(VirtualFile projectFile) {
    PantsProjectImportProvider provider = new PantsProjectImportProvider();
    AddModuleWizard dialog = new AddModuleWizard(null, projectFile.getPath(), provider);

    ProjectImportBuilder builder = provider.getBuilder();
    builder.setUpdate(false);
    dialog.getWizardContext().setProjectBuilder(builder);

    // dialog can only be shown in a non-headless environment
    Application application = ApplicationManager.getApplication();
    if (application.isHeadlessEnvironment() || dialog.showAndGet()) {
      return dialog;
    }
    else {
      return null;
    }
  }

  private Project createProject(VirtualFile file, AddModuleWizard dialog) {
    Project project = PantsUtil.findBuildRoot(file)
      .map(root -> Paths.get(root.getPath()))
      .map(root -> ProjectManagerEx.getInstanceEx().newProject(root, dialog.getProjectName(), new OpenProjectTask()))
      .orElse(null);
    if (project != null) {
      project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true);
    }
    return project;
  }

  private Path projectDir(VirtualFile file) {
    return file.isDirectory()
           ? Paths.get(file.getPath())
           : Paths.get(file.getParent().getPath());
  }
}
