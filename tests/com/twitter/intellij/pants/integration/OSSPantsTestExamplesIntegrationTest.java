// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.util.Pair;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.util.Optional;

public class OSSPantsTestExamplesIntegrationTest extends OSSPantsIntegrationTest {
  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "JUnit");
  }

  public void testJUnitTests() throws Throwable {
    doImport("intellij-integration/tests/java/org/pantsbuild/testprojects");

    assertFirstSourcePartyModules("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects");

    Pair<Boolean, Optional<String>> result = pantsCompileModule("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects");
    assertPantsCompileExecutesAndSucceeds(result);
    assertTrue(result.getSecond().isPresent());
    assertTrue(result.getSecond().get().contains("compile intellij-integration/tests/java/org/pantsbuild/testprojects:testprojects"));
    assertSuccessfulTest(
      "intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects", "org.pantsbuild.testprojects.JUnitIntegrationTest");
    final OSProcessHandler processHandler = runJUnitTest(
      "intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects",
      "org.pantsbuild.testprojects.JUnitIntegrationTest",
      "-DPANTS_FAIL_TEST=true"
    );
    assertTrue("Tests should fail!", processHandler.getProcess().exitValue() != 0);
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

    Pair<Boolean, Optional<String>> result = pantsCompileModule("testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher");
    assertPantsCompileExecutesAndSucceeds(result);
    assertTrue(result.getSecond().isPresent());
    String output = result.getSecond().get();
    // Make sure only matcher target is compiled
    assertContainsSubstring(output, matcherTarget);
    assertNotContainsSubstring(output, passingTarget);
    assertNotContainsSubstring(output, failingTarget);

    assertSuccessfulTest(
      "testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher",
      "org.pantsbuild.testproject.matcher.MatcherTest"
    );

    // Make sure only the 2 dummies targets are compiled.
    Pair<Boolean, Optional<String>>resultB = pantsCompileModule("testprojects_tests_java_org_pantsbuild_testproject_dummies_common_sources");
    String outputB = resultB.getSecond().get();
    assertContainsSubstring(outputB, passingTarget);
    assertContainsSubstring(outputB, failingTarget);
    assertNotContainsSubstring(outputB, matcherTarget);

    // makeProject() will result all 3 targets to be compiled.
    Pair<Boolean, Optional<String>> resultC = pantsCompileProject();
    assertPantsCompileExecutesAndSucceeds(resultC);
    String outputC = resultC.getSecond().get();
    assertContainsSubstring(outputC, passingTarget);
    assertContainsSubstring(outputC, failingTarget);
    assertContainsSubstring(outputC, matcherTarget);
  }
}
