// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectOpenProcessor;
import com.twitter.intellij.pants.service.project.wizard.FastpassProjectImportProvider;
import com.twitter.intellij.pants.service.project.wizard.PantsProjectImportBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

final class FastpassProjectOpenProcessor extends ProjectOpenProcessor {

  @Nls
  @NotNull
  @Override
  public String getName() {
    return "'" + FastpassProjectImportProvider.label() + "'";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return null;
  }

  @Override
  public boolean canOpenProject(@NotNull VirtualFile file) {
    return FastpassOpenProjectProvider.getInstance().canOpenProject(file);
  }

  @Nullable
  @Override
  public Project doOpenProject(
    @NotNull VirtualFile file, @Nullable Project projectToClose, boolean forceOpenInNewFrame
  ) {
    return FastpassOpenProjectProvider.getInstance().openProject(file, projectToClose, forceOpenInNewFrame);
  }

  @Override
  public boolean canImportProjectAfterwards() {
    return true;
  }

  @Override
  public void importProjectAfterwards(@NotNull Project project, @NotNull VirtualFile file) {
    FastpassOpenProjectProvider.getInstance().linkToExistingProject(file, project);
  }
}
