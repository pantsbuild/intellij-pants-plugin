// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.oss;

public class OSSPantsScalaExamplesIntegrationTest extends OSSPantsIntegrationTest {
  public void testHello() throws Throwable {
    doImport("examples/src/scala/com/pants/example/hello");

    assertModules(
      "examples_src_resources_com_pants_example_hello_hello",
      "examples_src_scala_com_pants_example_hello_module",
      "examples_src_scala_com_pants_example_hello_hello",
      "examples_src_scala_com_pants_example_hello_welcome_welcome",
      "examples_src_java_com_pants_examples_hello_greet_greet",
      "examples_src_scala_com_pants_example_hello_exe_exe"
    );

    makeModules("examples_src_scala_com_pants_example_hello_exe_exe");
    assertNotNull(
      findClassFile("com.pants.example.hello.exe.Exe", "examples_src_scala_com_pants_example_hello_exe_exe")
    );
    assertGotoFileContains("README");
    //Assert if README file under a sub directory is indexed.
    assertGotoFileContains("README_DOCS");
  }
}
