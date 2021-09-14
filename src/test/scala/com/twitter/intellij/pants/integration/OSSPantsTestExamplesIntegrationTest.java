// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.execution.PantsExecuteTaskResult;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.testFramework.RunResult;

public class OSSPantsTestExamplesIntegrationTest extends OSSPantsIntegrationTest {

  @Override
  protected boolean runInDispatchThread() {
    return false;
  }

  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "JUnit");
  }

  public void testJUnitTests() throws Throwable {
    doImport("intellij-integration/tests/java/org/pantsbuild/testprojects");

    assertFirstSourcePartyModules("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects");

    PantsExecuteTaskResult result = pantsCompileModule("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects");
    assertPantsCompileExecutesAndSucceeds(result);
    assertTrue(result.output.isPresent());
    assertTrue(result.output.get().contains("compile intellij-integration/tests/java/org/pantsbuild/testprojects:testprojects"));
    assertSuccessfulTest(
      "intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects", "org.pantsbuild.testprojects.JUnitIntegrationTest");
    RunResult runResult = runJUnitTest(
      "intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects",
      "org.pantsbuild.testprojects.JUnitIntegrationTest",
      "-DPANTS_FAIL_TEST=true"
    );
    assertTrue("Tests should fail!", runResult.getExitCode() != 0);
  }

  public void testScopedJUnitTests() throws Throwable {
    /**
     * Import 3 targets:
     * testprojects/tests/java/org/pantsbuild/testproject/dummies:passing_target
     * testprojects/tests/java/org/pantsbuild/testproject/dummies:failing_target
     * testprojects/tests/java/org/pantsbuild/testproject/matcher:matcher
     */
    doImport("testprojects/tests/java/org/pantsbuild/testproject/matcher");
    doImport("testprojects/tests/java/org/pantsbuild/testproject/dummies");

    String passingTarget = "testprojects/tests/java/org/pantsbuild/testproject/dummies:passing_target";
    String failingTarget = "testprojects/tests/java/org/pantsbuild/testproject/dummies:failing_target";
    String matcherTarget = "testprojects/tests/java/org/pantsbuild/testproject/matcher:matcher";

    PantsExecuteTaskResult result = pantsCompileModule("testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher");
    assertPantsCompileExecutesAndSucceeds(result);
    assertTrue(result.output.isPresent());
    String output = result.output.get();
    // Make sure only matcher target is compiled
    assertContainsSubstring(output, matcherTarget);
    assertNotContainsSubstring(output, passingTarget);
    assertNotContainsSubstring(output, failingTarget);

    assertSuccessfulTest(
      "testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher",
      "org.pantsbuild.testproject.matcher.MatcherTest"
    );

    // Make sure only the 2 dummies targets are compiled.
    PantsExecuteTaskResult resultB = pantsCompileModule("testprojects_tests_java_org_pantsbuild_testproject_dummies_common_sources");
    String outputB = resultB.output.get();
    assertContainsSubstring(outputB, passingTarget);
    assertContainsSubstring(outputB, failingTarget);
    assertNotContainsSubstring(outputB, matcherTarget);

    // makeProject() will result all 3 targets to be compiled.
    PantsExecuteTaskResult resultC = pantsCompileProject();
    assertPantsCompileExecutesAndSucceeds(resultC);
    String outputC = resultC.output.get();
    assertContainsSubstring(outputC, passingTarget);
    assertContainsSubstring(outputC, failingTarget);
    assertContainsSubstring(outputC, matcherTarget);
  }
}
