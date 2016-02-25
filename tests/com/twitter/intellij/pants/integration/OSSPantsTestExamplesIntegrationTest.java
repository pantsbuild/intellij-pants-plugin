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
    assertContainsSubstring(output, "compile intellij-integration/tests/java/org/pantsbuild/testprojects:testprojects --no-colors");
    assertSuccessfulJUnitTest("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects", "org.pantsbuild.testprojects.JUnitIntegrationTest");
    final OSProcessHandler processHandler = runJUnitTest(
      "intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects",
      "org.pantsbuild.testprojects.JUnitIntegrationTest",
      "-DPANTS_FAIL_TEST=true"
    );
    assertTrue("Tests should fail!", processHandler.getProcess().exitValue() != 0);
  }

  public void testScopedJUnitTests() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/dummies");
    doImport("testprojects/tests/java/org/pantsbuild/testproject/matcher");

    // Make sure only testprojects/tests/java/org/pantsbuild/testproject/matcher:matcher is compiled, not the whole project
    List<String> output = makeModules("testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher");
    assertContainsSubstring(output, "compile testprojects/tests/java/org/pantsbuild/testproject/matcher:matcher --no-colors");
    assertSuccessfulJUnitTest("testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher", "org.pantsbuild.testproject.matcher.MatcherTest");
  }

  private void assertContainsSubstring(List<String> stringList, String expected){
    for (String line: stringList) {
      if (line.contains(expected)) {
        return;
      }
    }
    fail(String.format("Compile output %s does not contain substring '%s'.", stringList.toString(), expected));
  }
}
