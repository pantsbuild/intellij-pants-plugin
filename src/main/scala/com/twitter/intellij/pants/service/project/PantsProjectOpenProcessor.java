// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectOpenProcessor;
import com.twitter.intellij.pants.service.project.wizard.PantsProjectImportBuilder;
import com.twitter.intellij.pants.service.project.wizard.PantsProjectImportProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

final class PantsProjectOpenProcessor extends ProjectOpenProcessor {
  @NotNull
  private PantsProjectImportBuilder getBuilder() {
    return PantsProjectImportBuilder.getInstance();
  }

  @Nls
  @NotNull
  @Override
  public String getName() {
    return "'" + PantsProjectImportProvider.label() + "'";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return getBuilder().getIcon();
  }

  @Override
  public boolean canOpenProject(@NotNull VirtualFile file) {
    return PantsOpenProjectProvider.getInstance().canOpenProject(file);
  }

  @Nullable
  @Override
  public Project doOpenProject(
    @NotNull VirtualFile file, @Nullable Project projectToClose, boolean forceOpenInNewFrame
  ) {
    return PantsOpenProjectProvider.getInstance().openProject(file, projectToClose, forceOpenInNewFrame);
  }

  @Override
  public boolean canImportProjectAfterwards() {
    return true;
  }

  @Override
  public void importProjectAfterwards(@NotNull Project project, @NotNull VirtualFile file) {
    PantsOpenProjectProvider.getInstance().linkToExistingProject(file, project);
  }
}
