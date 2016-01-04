// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

  import com.intellij.execution.process.OSProcessHandler;
  import com.intellij.util.ArrayUtil;
  import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class OSSPantsTestExamplesIntegrationTest extends OSSPantsIntegrationTest {
  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "JUnit");
  }

  public void testJUnitTests() throws Throwable {
    doImport("intellij-integration/tests/java/org/pantsbuild/testprojects");

    assertModules("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects");

    makeModules("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects");
    assertSuccessfulJUnitTest("intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects", "org.pantsbuild.testprojects.JUnitIntegrationTest");
    final OSProcessHandler processHandler = runJUnitTest(
      "intellij-integration_tests_java_org_pantsbuild_testprojects_testprojects",
      "org.pantsbuild.testprojects.JUnitIntegrationTest",
      "-DPANTS_FAIL_TEST=true"
    );
    assertTrue("Tests should fail!", processHandler.getProcess().exitValue() != 0);
  }
}
