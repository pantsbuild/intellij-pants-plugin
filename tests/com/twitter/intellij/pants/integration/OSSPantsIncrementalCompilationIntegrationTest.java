// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class OSSPantsIncrementalCompilationIntegrationTest extends OSSPantsIntegrationTest {
  public OSSPantsIncrementalCompilationIntegrationTest() {
    super(false);
  }

  public void testHelloByTargetName() throws Throwable {
    doImport("examples/src/scala/org/pantsbuild/example/hello/BUILD", "hello");

    assertModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_scala_org_pantsbuild_example_hello_module",
      "examples_src_scala_org_pantsbuild_example_hello_hello",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_scala_org_pantsbuild_example_hello_exe_exe"
    );

    assertContain(makeProject(), "pants: Recompiling all 5 targets");
    assertContain(makeProject(), "pants: No changes to compile.");

    modify("org.pantsbuild.example.hello.exe.Exe");

    assertContain(makeProject(), "pants: Recompiling all 5 targets");

    modify("org.pantsbuild.example.hello.exe.Exe");
    modify("org.pantsbuild.example.hello.welcome.WelcomeEverybody");

    assertContain(makeProject(), "pants: Recompiling all 5 targets");
  }
}
