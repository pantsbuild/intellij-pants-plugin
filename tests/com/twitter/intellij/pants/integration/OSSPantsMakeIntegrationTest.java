// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.junit.JUnitConfiguration;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;


public class OSSPantsMakeIntegrationTest extends OSSPantsIntegrationTest {

  public void testPantsMake() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/");


    JUnitConfiguration runConfiguration = generateJUnitConfiguration(
      "testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher", "org.pantsbuild.testproject.matcher.MatcherTest", null);

    assertAndRunPantsMake(runConfiguration);
    assertSuccessfulJUnitTest(runConfiguration);
  }

  public void testCompileAll() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/annotation");
    doImport("testprojects/tests/java/org/pantsbuild/testproject/cwdexample");


    JUnitConfiguration runConfiguration = generateJUnitConfiguration(
      "testprojects_tests_java_org_pantsbuild_testproject_annotation_annotation",
      "org.pantsbuild.testproject.annotation.AnnotationTest",
      null
    );

    assertCompileAll();
    assertSuccessfulJUnitTest(runConfiguration);
  }

}
