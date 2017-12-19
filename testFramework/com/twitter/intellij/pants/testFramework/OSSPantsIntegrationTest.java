// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.java.LanguageLevel;
import com.twitter.intellij.pants.util.PantsUtil;
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

  protected void assertContainsSubstring(List<String> stringList, String expected) {
    assertContainsSubstring(StringUtil.join(stringList, ""), expected);
  }

  protected void assertContainsSubstring(String s, String expected) {
    if (s.contains(expected)) {
      return;
    }
    fail(String.format("String '%s' does not contain expected substring '%s'.", s, expected));
  }

  protected void assertNotContainsSubstring(List<String> stringList, String unexpected) {
    assertNotContainsSubstring(StringUtil.join(stringList, ""), unexpected);
  }

  protected void assertNotContainsSubstring(String s, String unexpected) {
    if (!s.contains(unexpected)) {
      return;
    }
    fail(String.format("String '%s' contains unexpected substring '%s'.", s, unexpected));
  }

  private List<BeforeRunTask<?>> getBeforeRunTask(RunConfiguration configuration) {
    RunManagerImpl runManager = (RunManagerImpl) RunManager.getInstance(myProject);
    RunnerAndConfigurationSettingsImpl configurationSettings = new RunnerAndConfigurationSettingsImpl(runManager, configuration, true);
    runManager.addConfiguration(configurationSettings, true);
    List<BeforeRunTask<?>> tasks = runManager.getBeforeRunTasks(configuration);
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

  /**
   * Assert Project has the right JDK and language level (JVM project only).
   */
  protected void assertProjectJdkAndLanguageLevel() {
    final String pantsExecutablePath = PantsUtil.findPantsExecutable(getParentPath()).get().getPath();
    assertEquals(
      ProjectRootManager.getInstance(myProject).getProjectSdk().getHomePath(),
      getDefaultJavaSdk(pantsExecutablePath).get().getHomePath()
    );

    LanguageLevel projectLanguageLevel = LanguageLevelProjectExtension.getInstance(myProject).getLanguageLevel();
    LanguageLevel expectedLanguageLevel = LanguageLevel.JDK_1_8;
    assertTrue(
      String.format("Project Language Level should be %s, but is %s", expectedLanguageLevel, projectLanguageLevel),
      projectLanguageLevel.equals(LanguageLevel.JDK_1_8)
    );
  }
}
