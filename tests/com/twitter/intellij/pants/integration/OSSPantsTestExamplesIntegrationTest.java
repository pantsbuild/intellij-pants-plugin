// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.util.List;

public class OSSPantsTestExamplesIntegrationTest extends OSSPantsIntegrationTest {
  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "JUnit");
  }

  public void testJUnitTests() throws Throwable {
    doImport("intellij-integration/tests/java/org/pantsbuild/testprojects");

    assertModules("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects");

    List<String> output = makeModules("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects");
    assertContains(output, "compile intellij-integration/tests/java/org/pantsbuild/testprojects:testprojects --no-colors");
    assertSuccessfulJUnitTest("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects", "org.pantsbuild.testprojects.JUnitIntegrationTest");
    final OSProcessHandler processHandler = runJUnitTest(
      "intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects",
      "org.pantsbuild.testprojects.JUnitIntegrationTest",
      "-DPANTS_FAIL_TEST=true"
    );
    assertTrue("Tests should fail!", processHandler.getProcess().exitValue() != 0);
  }

  public void testScopedJUnitTests() throws Throwable {
    //doImport("testprojects/tests/java/org/pantsbuild/testproject/dummies");
    doImport("testprojects/tests/java/org/pantsbuild/testproject/matcher");

    //assertModules("testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher");
    //assertContains(output, "compile testprojects/tests/java/org/pantsbuild/testproject/matcher:matcher --no-colors");
    //Module: 'testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher'
    makeProject();
    modify("org.pantsbuild.testproject.matcher.MatcherTest");
    List<String> output = makeModules("testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher");
    assertSuccessfulJUnitTest("testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher", "org.pantsbuild.testproject.matcher.MatcherTest");

    //List<String> output = makeModules("testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher");

    //final OSProcessHandler processHandler = runJUnitTest(
    //  "intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects",
    //  "org.pantsbuild.testprojects.JUnitIntegrationTest",
    //  "-DPANTS_FAIL_TEST=true"
    //);
    int x = 5;
    //assertTrue("Tests should fail!", processHandler.getProcess().exitValue() != 0);
  }

  private void assertContains(List<String> stringList, String expected){
    for (String line: stringList) {
      if (line.contains(expected)) {
        return;
      }
    }
    fail(String.format("compile output does not contain '%s'", expected));
  }
}
