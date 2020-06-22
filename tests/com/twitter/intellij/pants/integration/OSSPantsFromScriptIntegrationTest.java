// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class OSSPantsFromScriptIntegrationTest extends OSSPantsIntegrationTest {
  public void testScript() throws Throwable {
    doImport("intellij-integration/export1.sh");

    assertProjectName("pants.intellij-integration.export1.sh::");

    assertFirstSourcePartyModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_main_main",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_scala_org_pantsbuild_example_hello_hello",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome",
      "examples_src_scala_org_pantsbuild_example_hello_exe_exe",
      "examples_src_java_org_pantsbuild_example_hello_main_readme",
      "examples_src_java_org_pantsbuild_example_hello_main_common_sources",
      "export1_module",
      "intellij-integration_export1.sh-project-root"
    );

    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
  }
}
