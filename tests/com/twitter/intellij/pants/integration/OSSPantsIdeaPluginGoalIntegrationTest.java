// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.ide.highlighter.ProjectFileType;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.util.ui.UIUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;


public class OSSPantsIdeaPluginGoalIntegrationTest extends OSSPantsIntegrationTest {

  public void testPantsIdeaPluginGoal() throws Throwable {
    assertEmpty(ModuleManager.getInstance(myProject).getModules());

    /**
     * Check whether Pants supports `idea-plugin` goal.
     */
    PantsUtil.findPantsExecutable(getProjectFolder().getPath());
    final GeneralCommandLine commandLinePantsGoals = PantsUtil.defaultCommandLine(getProjectFolder().getPath());
    commandLinePantsGoals.addParameter("goals");
    final ProcessOutput cmdOutputGoals = PantsUtil.getCmdOutput(commandLinePantsGoals.withWorkDirectory(getProjectFolder()), null);
    assertEquals(commandLinePantsGoals.toString() + " failed", 0, cmdOutputGoals.getExitCode());
    if (!cmdOutputGoals.getStdout().contains("idea-plugin")) {
      return;
    }

    /**
     * Generate idea project via `idea-plugin` goal.
     */
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(getProjectFolder().getPath());
    final File outputFile = FileUtil.createTempFile("project_dir_location", ".out");
    commandLine.addParameters(
      "idea-plugin",
      "--no-open",
      "--output-file=" + outputFile.getPath(),
      "testprojects/tests/java/org/pantsbuild/testproject/::"
    );
    final ProcessOutput cmdOutput = PantsUtil.getCmdOutput(commandLine.withWorkDirectory(getProjectFolder()), null);
    assertEquals(commandLine.toString() + " failed", 0, cmdOutput.getExitCode());
    // `outputFile` contains the path to the project directory.
    String projectDir = FileUtil.loadFile(outputFile);

    // Search the directory for ipr file.
    File[] files = new File(projectDir).listFiles();
    assertNotNull(files);
    Optional<String> iprFile = Arrays.stream(files)
      .map(File::getPath)
      .filter(s -> s.endsWith(ProjectFileType.DOT_DEFAULT_EXTENSION))
      .findFirst();
    assertTrue(iprFile.isPresent());

    myProject = ProjectUtil.openProject(iprFile.get(), myProject, false);
    // Invoke post startup activities.
    UIUtil.dispatchAllInvocationEvents();
    /**
     * Under unit test mode, {@link com.intellij.ide.impl.ProjectUtil#openProject} will force open a project in a new window,
     * so Project SDK has to be reset. In practice, this is not needed.
     */
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        final JavaSdk javaSdk = JavaSdk.getInstance();
        ProjectRootManager.getInstance(myProject).setProjectSdk(ProjectJdkTable.getInstance().getSdksOfType(javaSdk).iterator().next());
      }
    });

    JUnitConfiguration runConfiguration = generateJUnitConfiguration(
      "testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher", "org.pantsbuild.testproject.matcher.MatcherTest", null);

    assertAndRunPantsMake(runConfiguration);
    assertSuccessfulJUnitTest(runConfiguration);
  }

  /**
   * Test framework cannot properly dispose newly opened project.
   * This is a hack to manually trigger disposal and ignore all disposal errors.
   */
  @Override
  public void tearDown() throws Exception {
    PlatformTestCase.closeAndDisposeProjectAndCheckThatNoOpenProjects(myProject, new ArrayList<Throwable>());
    cleanProjectRoot();
  }
}
