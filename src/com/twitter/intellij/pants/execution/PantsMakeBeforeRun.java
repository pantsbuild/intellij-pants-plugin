// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


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




    if (runConfiguration instanceof JUnitConfiguration ) {


      //RunManager runManager = RunManager.getInstance(myProject);
      //if (runManager instanceof RunManagerImpl) {
      //  RunManagerImpl runManagerImpl = (RunManagerImpl) runManager;
      //  List<BeforeRunTask> beforeRunTasks = runManagerImpl.getBeforeRunTasks(runConfiguration);
      //  //List<BeforeRunTask> newTasks = Arrays.asList(beforeRunTasks.iterator().next());
      //
      //
      //  //RunManagerEx.getInstanceEx(myProject).setBeforeRunTasks(runConfiguration, Collections.<BeforeRunTask>emptyList(), false);
      //  //runManagerImpl.setBeforeRunTasks(runConfiguration, newTasks, true);
      //
      //  //beforeRunTasks.add(new MyBeforeRunTask(providerID));
      //}



      JUnitConfiguration runConfig = ((JUnitConfiguration)runConfiguration);
      VirtualFile buildRoot = PantsUtil.findBuildRoot(myProject);
      if (buildRoot!=null) {
        runConfig.setWorkingDirectory(buildRoot.getCanonicalPath());
      }
      ExternalSystemBeforeRunTask pantsTask = new ExternalSystemBeforeRunTask(ID, PantsConstants.SYSTEM_ID);
      pantsTask.setEnabled(true);
      return pantsTask;
    }
    else if (runConfiguration instanceof ScalaTestRunConfiguration) {
      return null;
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
