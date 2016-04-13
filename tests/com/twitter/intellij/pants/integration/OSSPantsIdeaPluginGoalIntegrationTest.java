// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.PlatformTestCase;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;

import java.io.File;
import java.util.ArrayList;


public class OSSPantsIdeaPluginGoalIntegrationTest extends OSSPantsIntegrationTest {

  public void testPantsIdeaPluginGoal() throws Throwable {
    assertEmpty(ModuleManager.getInstance(myProject).getModules());
    PantsUtil.findPantsExecutable(getProjectFolder().getPath());
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
    String projectDir = FileUtil.loadFile(outputFile);

    myProject = ProjectUtil.openProject(projectDir, myProject, false);

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
   * Since the project is not in a temporary dir, it needs to disposed explicitly.
   *
   * @throws Exception
   */
  @Override
  public void tearDown() throws Exception {
    /**
     * Test framework cannot dispose the new project opened properly.
     * This is a hack to manually trigger disposal and ignore all disposal errors.
     */
    PlatformTestCase.closeAndDisposeProjectAndCheckThatNoOpenProjects(myProject, new ArrayList<Throwable>());
    cleanProjectRoot();
  }
}
