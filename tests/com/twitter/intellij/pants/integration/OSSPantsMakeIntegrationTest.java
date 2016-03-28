// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.util.List;


public class OSSPantsMakeIntegrationTest extends OSSPantsIntegrationTest {
  public void testPantsMake() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/");

    RunManager runManager = RunManager.getInstance(myProject);
    assertTrue(runManager instanceof RunManagerImpl);

    RunManagerImpl runManagerImpl = (RunManagerImpl) runManager;
    JUnitConfiguration runConfiguration = generateJUnitConfiguration(
      "testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher", "org.pantsbuild.testproject.matcher.MatcherTest", null);

    RunnerAndConfigurationSettings runnerAndConfigurationSettings =
      runManager.createConfiguration(runConfiguration, JUnitConfigurationType.getInstance().getConfigurationFactories()[0]);
    runManager.addConfiguration(runnerAndConfigurationSettings, false);

    // Make sure PantsMake is the one and only task before JUnit run.
    List<BeforeRunTask> beforeRunTaskList = runManagerImpl.getBeforeRunTasks(runConfiguration);
    assertEquals(1, beforeRunTaskList.size());
    BeforeRunTask task = beforeRunTaskList.iterator().next();
    assertEquals(PantsMakeBeforeRun.ID, task.getProviderId());

    /*
     * Manually invoke BeforeRunTask as {@link ExecutionManager#compileAndRun} launches another task asynchronously,
     * and there is no way to catch that.
     */
    BeforeRunTaskProvider<BeforeRunTask> provider = BeforeRunTaskProvider.getProvider(myProject, task.getProviderId());
    assertNotNull("Cannot find BeforeRunTaskProvider for id='" + task.getProviderId() + "'", provider);

    assertTrue(provider.executeTask(null, runConfiguration, null, task));
    assertSuccessfulJUnitTest(runConfiguration);
  }
}
