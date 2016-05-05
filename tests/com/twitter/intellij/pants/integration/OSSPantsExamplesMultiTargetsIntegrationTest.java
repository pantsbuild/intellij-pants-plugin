// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class OSSPantsExamplesMultiTargetsIntegrationTest extends OSSPantsIntegrationTest {
  public void testEmpty() {
    // Unfortunately this is in junit 3 we need at least one method starts with 'test'.
    // Remove once https://github.com/pantsbuild/intellij-pants-plugin/issues/133 is addressed.
  }

  // TODO (peiyu) https://github.com/pantsbuild/intellij-pants-plugin/issues/133
  public void IGNORE_testHello() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/hello");

    assertModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_main_main",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module"
    );

    makeModules("examples_src_java_org_pantsbuild_example_hello_main_main");
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.greet.Greeting", "examples_src_java_org_pantsbuild_example_hello_greet_greet"
    );

    doImport("examples/src/scala/org/pantsbuild/example/hello/BUILD", "hello");

    assertModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_main_main",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module",
      "examples_src_scala_org_pantsbuild_example_hello_module",
      "examples_src_scala_org_pantsbuild_example_hello_hello",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome",
      "examples_src_scala_org_pantsbuild_example_hello_exe_exe"
    );

    assertContainsSubstring(makeProject(), "Compiling 8 targets");

    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.welcome.WelcomeEverybody", "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome"
    );
  }
}
