// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsConstants;

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
    assertContainsSubstring(output, "compile intellij-integration/tests/java/org/pantsbuild/testprojects:testprojects");
    assertSuccessfulJUnitTest(
      "intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects", "org.pantsbuild.testprojects.JUnitIntegrationTest");
    final OSProcessHandler processHandler = runJUnitTest(
      "intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects",
      "org.pantsbuild.testprojects.JUnitIntegrationTest",
      "-DPANTS_FAIL_TEST=true"
    );
    assertTrue("Tests should fail!", processHandler.getProcess().exitValue() != 0);
  }

  public void testScopedJUnitTests() throws Throwable {
    ///**
    // * Import 3 targets:
    // * testprojects/tests/java/org/pantsbuild/testproject/dummies:passing_target
    // * testprojects/tests/java/org/pantsbuild/testproject/dummies:failing_target
    // * testprojects/tests/java/org/pantsbuild/testproject/matcher:matcher
    // */
    //doImport("testprojects/tests/java/org/pantsbuild/testproject/matcher");
    //doImport("testprojects/tests/java/org/pantsbuild/testproject/dummies");
    //
    //List<String> output = makeModules("testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher");
    //// Make sure only matcher target is compiled
    //assertContainsSubstring(output, "compile testprojects/tests/java/org/pantsbuild/testproject/matcher:matcher");
    //assertSuccessfulJUnitTest(
    //  "testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher", "org.pantsbuild.testproject.matcher.MatcherTest");
    //
    //// Make sure only the 2 dummies targets are compiled.
    //assertContainsSubstring(
    //  makeModules("_testprojects_tests_java_org_pantsbuild_testproject_dummies_common_sources"), "Compiling 2 targets");
    //
    //// makeProject() will result all 3 targets to be compiled.
    //assertContainsSubstring(makeProject(), "Compiling 3 targets");
  }
}
