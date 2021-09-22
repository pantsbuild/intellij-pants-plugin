// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.python.component;

import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

public class PantsPythonRunConfigListener implements RunManagerListener {
  private final Project myProject;

  public PantsPythonRunConfigListener(Project project) {
    myProject = project;
  }

  @Override
  public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
    if (myProject.isDefault() || !PantsUtil.isPantsProject(myProject)) {
      return;
    }

    final RunConfiguration runConfiguration = settings.getConfiguration();
    if (runConfiguration instanceof AbstractPythonRunConfiguration) {
      AbstractPythonRunConfiguration<?> configuration = (AbstractPythonRunConfiguration<?>) runConfiguration;
      String workingDirectory = configuration.getWorkingDirectory();
      if (StringUtil.isEmpty(workingDirectory)) {
        PantsUtil.findBuildRoot(myProject)
          .map(VirtualFile::getCanonicalPath)
          .ifPresent(configuration::setWorkingDirectory);
      }
    }
  }
}
