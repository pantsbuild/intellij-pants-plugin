// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.List;

abstract public class OSSPantsIntegrationTest extends PantsIntegrationTestCase {
  public OSSPantsIntegrationTest() {
  }

  public OSSPantsIntegrationTest(boolean readOnly) {
    super(readOnly);
  }

  @NotNull
  @Override
  protected List<File> getProjectFoldersToCopy() {
    final File testProjects = new File(PantsTestUtils.findTestPath("testData"), "testprojects");
    return Collections.singletonList(testProjects);
  }

  @NotNull
  @Override
  protected File getProjectFolder() {
    final String ossPantsHome = System.getenv("OSS_PANTS_HOME");
    if (!StringUtil.isEmpty(ossPantsHome)) {
      return new File(ossPantsHome);
    }
    final File workingDir = PantsTestUtils.findTestPath("testData").getParentFile();
    return new File(workingDir.getParent(), "pants");
  }

  protected void assertContainsSubstring(List<String> stringList, String expected) {
    if (containsSubstring(stringList, expected)) {
      return;
    }
    fail(String.format("String '%s' does not contain expected substring '%s'.", stringList.toString(), expected));
  }

  protected void assertNotContainsSubstring(List<String> stringList, String unexpected) {
    if (!containsSubstring(stringList, unexpected)) {
      return;
    }
    fail(String.format("String '%s' contains unexpected substring '%s'.", stringList.toString(), unexpected));
  }

  private boolean containsSubstring(List<String> stringList, String subString) {
    return stringList.stream().anyMatch(s -> s.contains(subString));
  }

  protected void assertCompileAll() {
    PantsMakeBeforeRun runner = new PantsMakeBeforeRun(myProject);
    assertTrue(runner.executeTask(myProject));
  }

  private List<BeforeRunTask> getBeforeRunTask(RunConfiguration configuration) {
    RunManagerImpl runManager = (RunManagerImpl) RunManager.getInstance(myProject);
    RunnerAndConfigurationSettingsImpl configurationSettings = new RunnerAndConfigurationSettingsImpl(runManager, configuration, true);
    runManager.addConfiguration(configurationSettings, true);
    List<BeforeRunTask> tasks = runManager.getBeforeRunTasks(configuration);
    runManager.removeConfiguration(configurationSettings);
    return tasks;
  }

  /**
   * Assert `configuration` contains no before-run task such as Make or PantsMakeBeforeRun.
   *
   * @param configuration to add to the project.
   */
  protected void assertEmptyBeforeRunTask(RunConfiguration configuration) {
    assertEmpty(getBeforeRunTask(configuration));
  }
}
