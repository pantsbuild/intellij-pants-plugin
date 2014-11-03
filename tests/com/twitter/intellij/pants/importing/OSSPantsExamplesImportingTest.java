// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.importing;

import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.util.PantsTestUtils;

import java.io.File;

public class OSSPantsExamplesImportingTest extends PantsImportingTestCase {
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
  }
}
