// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.python.component.impl;

import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.twitter.intellij.pants.service.python.component.PantsPythonProjectComponent;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;


public class PantsPythonProjectComponentImpl extends AbstractProjectComponent implements PantsPythonProjectComponent {
  protected PantsPythonProjectComponentImpl(Project project) {
    super(project);
  }

  @Override
  public void projectOpened() {
    super.projectOpened();
    if (myProject.isDefault()) {
      return;
    }
    myProject.getMessageBus().connect().subscribe(RunManagerListener.TOPIC, new RunManagerListener() {
      @Override
      public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
        final RunConfiguration runConfiguration = settings.getConfiguration();
        if (!(runConfiguration instanceof AbstractPythonRunConfiguration)) {
          return;
        }
        final String workingDirectory = ((AbstractPythonRunConfiguration) runConfiguration).getWorkingDirectory();
        if (StringUtil.isEmpty(workingDirectory)) {
          final Optional<VirtualFile> projectBuildRoot = PantsUtil.findBuildRoot(myProject);
          projectBuildRoot
            .ifPresent(file -> ((AbstractPythonRunConfiguration) runConfiguration).setWorkingDirectory(file.getCanonicalPath()));
        }
      }
    });
  }
}
