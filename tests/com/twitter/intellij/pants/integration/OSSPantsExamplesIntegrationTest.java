// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.util.PantsTestUtils;

import java.io.File;

public class OSSPantsExamplesIntegrationTest extends PantsIntegrationTestCase {
  @Override
  protected File getProjectFolderToCopy() {
    final String ossPantsHome = System.getenv("OSS_PANTS_HOME");
    if (!StringUtil.isEmpty(ossPantsHome)) {
      return new File(ossPantsHome);
    }
    final File workingDir = PantsTestUtils.findTestPath("testData").getParentFile();
    return new File(workingDir.getParent(), "pants");
  }

  public void testHello() throws Throwable {
    doTest("examples/src/java/com/pants/examples/hello");

    assertModules(
      "examples_src_resources_com_pants_example_hello_hello",
      "examples_src_java_com_pants_examples_hello_main_readme",
      "examples_src_java_com_pants_examples_hello_main_main",
      "examples_src_java_com_pants_examples_hello_greet_greet",
      "examples_src_java_com_pants_examples_hello_main_main-bin"
    );

    makeModules("examples_src_java_com_pants_examples_hello_main_main");
    assertNotNull(
      findClassFile("com.pants.examples.hello.greet.Greeting", "examples_src_java_com_pants_examples_hello_greet_greet")
    );
    assertNotNull(
      findClassFile("com.pants.examples.hello.main.HelloMain", "examples_src_java_com_pants_examples_hello_main_main-bin")
    );
  }
}
