// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.importing.OpenProjectProvider;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.bsp.FastpassUtils;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.bsp.project.importing.BspProjectImportBuilder;
import org.jetbrains.bsp.project.importing.setup.FastpassConfigSetup;
import org.jetbrains.plugins.scala.build.BuildReporter;
import org.jetbrains.plugins.scala.build.IndicatorReporter;
import scala.util.Try;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static com.intellij.ide.impl.ProjectUtil.focusProjectWindow;
import static com.intellij.ide.impl.ProjectUtil.isSameProject;
import static com.twitter.intellij.pants.bsp.FastpassUtils.fastpassBinaryExists;

final class FastpassOpenProjectProvider implements OpenProjectProvider {
  private static final FastpassOpenProjectProvider instance = new FastpassOpenProjectProvider();

  static OpenProjectProvider getInstance() {
    return instance;
  }

  @Override
  public boolean canOpenProject(@NotNull VirtualFile file) {
    Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(file);

    if (!buildRoot.isPresent()) return false;

    return fastpassBinaryExists(file) && (file.isDirectory() || PantsUtil.isBUILDFileName(file.getName()));
  }


  @Override
  public void linkToExistingProject(@NotNull VirtualFile projectFile, @NotNull Project project) {
    link(projectFile);
  }

  @Nullable
  @Override
  public Project openProject(@NotNull VirtualFile projectFile, @Nullable Project projectToClose, boolean forceOpenInNewFrame) {
    Path bspWorkspaceDirectory = bspWorkspaceDir(projectFile);
    if(bspWorkspaceDirectory == null) return null;
    if (isAlreadyOpen(bspWorkspaceDirectory)) return null;
    Project project = importFastpassProject(projectFile);
    if(project != null) {
      OpenProjectTask task = PantsOpenProjectProvider.openTask(projectToClose, project, forceOpenInNewFrame);
      ProjectManagerEx.getInstanceEx().openProject(bspWorkspaceDirectory, task);
    }
    return project;
  }

  private boolean isAlreadyOpen(Path projectDirectory) {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      if (isSameProject(projectDirectory, project)) {
        focusProjectWindow(project, false);
        return true;
      }
    }
    return false;
  }

  @Nullable
  private Project importFastpassProject(@NotNull VirtualFile projectFile) {
    return link(projectFile);
  }

  private Project link(@NotNull VirtualFile projectFile) {
    Path bspPath = bspWorkspaceDir(projectFile);
    if(bspPath == null) {
      return null;
    }
    Project project = ProjectManagerEx.getInstanceEx().newProject(bspPath, new OpenProjectTask());
    if(project != null) {
      Task.Modal t = new Task.Modal(project, "Creating Fastpass Project", false) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          BspProjectImportBuilder builder = new BspProjectImportBuilder();
          try {
            Try<FastpassConfigSetup> fastpassConfigSetup = FastpassConfigSetup.create(new File(projectFile.getPath()))
              .map(x -> (FastpassConfigSetup) x);
            BuildReporter reporter = new IndicatorReporter(indicator);
            fastpassConfigSetup.get().run(reporter);
            ApplicationManager.getApplication().invokeAndWait(
              () -> ApplicationManager.getApplication().runWriteAction(() -> {
                builder.setExternalBspWorkspace(bspPath);
                builder.commit(project, null, ModulesProvider.EMPTY_MODULES_PROVIDER);
                project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, Boolean.TRUE);
                project.save();
              }));
          }
          finally {
            builder.cleanup();
          }
        }
      };
      ProgressManager.getInstance().run(t);
    }
    return project;
  }

  private Path bspWorkspaceDir(VirtualFile file) {
    if(file.getCanonicalPath() == null) {
      return null;
    }
    return FastpassConfigSetup.computeBspWorkspace(Paths.get(file.getCanonicalPath()).toFile()).toAbsolutePath();
  }
}
