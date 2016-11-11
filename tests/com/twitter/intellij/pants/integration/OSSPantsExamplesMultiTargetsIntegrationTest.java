// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import org.apache.commons.lang.ArrayUtils;

public class OSSPantsExamplesMultiTargetsIntegrationTest extends OSSPantsIntegrationTest {
  public void testHelloJavaAndScala() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/hello");

    assertProjectName("pants.examples.src.java.org.pantsbuild.example.hello::");

    String[] initialModules = {"examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_main_main",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module",
      "examples_src_java_org_pantsbuild_example_hello_main_readme",
      "examples_src_java_org_pantsbuild_example_hello_main_common_sources"
    };

    assertFirstSourcePartyModules(initialModules);

    assertPantsCompileExecutesAndSucceeds(pantsCompileModule("examples_src_java_org_pantsbuild_example_hello_main_main"));

    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.greet.Greeting", "examples_src_java_org_pantsbuild_example_hello_greet_greet"
    );

    doImport("examples/src/scala/org/pantsbuild/example/hello/BUILD", "hello");
    assertProjectName("pants.examples.src.scala.org.pantsbuild.example.hello:hello");

    String[] additionalModules = {
      "examples_src_scala_org_pantsbuild_example_hello_module",
      "examples_src_scala_org_pantsbuild_example_hello_hello",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome",
      "examples_src_scala_org_pantsbuild_example_hello_exe_exe"
    };

    assertFirstSourcePartyModules((String[]) ArrayUtils.

      addAll(initialModules, additionalModules));

    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());

    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.welcome.WelcomeEverybody", "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome"
    );
  }
}
