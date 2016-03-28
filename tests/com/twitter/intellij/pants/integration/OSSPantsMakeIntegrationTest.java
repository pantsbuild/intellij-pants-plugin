// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsConstants;

import java.util.Collections;
import java.util.List;


public class OSSPantsMakeIntegrationTest extends OSSPantsIntegrationTest {
  public void testPantsMake() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/matcher");
    //doImport("testprojects/tests/java/org/pantsbuild/testproject/dummies");

    RunManager runManager = RunManager.getInstance(myProject);
    if (!(runManager instanceof RunManagerImpl)) {
      return;
    }
    RunManagerImpl runManagerImpl = (RunManagerImpl) runManager;

    //BeforeRunTask pantsMakeTask = new ExternalSystemBeforeRunTask(PantsMakeBeforeRun.ID, PantsConstants.SYSTEM_ID);
    //pantsMakeTask.setEnabled(true);

    JUnitConfiguration runConfiguration = generateJUnitConfiguration(
      "testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher", "org.pantsbuild.testproject.matcher.MatcherTest", null);

    //runManagerImpl.setBeforeRunTasks(runConfiguration, Collections.singletonList(pantsMakeTask), false);

    RunnerAndConfigurationSettings runnerAndConfigurationSettings =
      runManagerImpl.createConfiguration(runConfiguration, JUnitConfigurationType.getInstance().getConfigurationFactories()[0]);
    runManagerImpl.addConfiguration(runnerAndConfigurationSettings, false);

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
