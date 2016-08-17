// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class OSSPantsExamplesMultiTargetsIntegrationTest extends OSSPantsIntegrationTest {
  public void testHelloJavaAndScala() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/hello");

    assertProjectName("examples/src/java/org/pantsbuild/example/hello::");
    assertSourceModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module"
    );

    assertPantsCompileModule("examples_src_java_org_pantsbuild_example_hello_main_main");
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.greet.Greeting", "examples_src_java_org_pantsbuild_example_hello_greet_greet"
    );

    doImport("examples/src/scala/org/pantsbuild/example/hello/BUILD", "hello");
    assertProjectName("examples/src/scala/org/pantsbuild/example/hello:hello");

    assertSourceModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module",
      "examples_src_scala_org_pantsbuild_example_hello_module",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome",
      "examples_src_scala_org_pantsbuild_example_hello_exe_exe"
    );

    assertPantsCompileAll();

    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.welcome.WelcomeEverybody", "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome"
    );
  }
}
