// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;

import javax.swing.*;


public class PantsMakeBeforeRun extends ExternalSystemBeforeRunTaskProvider {
  public static final Key<ExternalSystemBeforeRunTask> ID = Key.create("Pants.BeforeRunTask");
  public Project myProject;

  public PantsMakeBeforeRun(@NotNull Project project) {
    super(PantsConstants.SYSTEM_ID, project, ID);
    myProject = project;
  }

  @Override
  public String getName() {
    return "PantsCompile";
  }

  @Override
  public String getDescription(ExternalSystemBeforeRunTask task) {
    return "PantsCompile";
  }

  @Nullable
  @Override
  public ExternalSystemBeforeRunTask createTask(RunConfiguration runConfiguration) {
    if (runConfiguration instanceof JUnitConfiguration || runConfiguration instanceof ScalaTestRunConfiguration) {
      ExternalSystemBeforeRunTask pantsTask = new ExternalSystemBeforeRunTask(ID, PantsConstants.SYSTEM_ID);
      pantsTask.setEnabled(true);
      return pantsTask;
    }
    else {
      return null;
    }
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return PantsIcons.Icon;
  }

  @Override
  public boolean canExecuteTask(
    RunConfiguration configuration, ExternalSystemBeforeRunTask beforeRunTask
  ) {
    return true;
  }

  @Override
  public boolean executeTask(
    DataContext context,
    RunConfiguration configuration,
    ExecutionEnvironment env,
    ExternalSystemBeforeRunTask beforeRunTask
  ) {
    System.out.println("Pants Make");
    return true;
  }
}
