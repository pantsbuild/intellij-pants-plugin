// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.compiler.CompilerMessage;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsConstants;
import org.junit.Ignore;

import java.util.List;

public class ExternalBuilderIntegrationTest extends OSSPantsIntegrationTest {
  @Ignore("Hang on dead lock: https://github.com/pantsbuild/intellij-pants-plugin/issues/382")
  public void testExternalBuilderError() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/hello");

    assertFirstSourcePartyModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_main_main",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module",
      "examples_src_java_org_pantsbuild_example_hello_main_readme",
      "examples_src_java_org_pantsbuild_example_hello_main_common_sources"
    );

    List<CompilerMessage> make = getCompilerTester().make();
    assertContainsSubstring(
      make.iterator().next().getMessage(),
      PantsConstants.EXTERNAL_BUILDER_ERROR
    );
  }
}
