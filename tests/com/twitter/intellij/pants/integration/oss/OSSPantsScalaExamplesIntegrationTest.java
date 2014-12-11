// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.oss;

import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

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

    assertModuleModuleDeps(
      "examples_src_scala_com_pants_example_hello_exe_exe",
      "examples_src_scala_com_pants_example_hello_welcome_welcome"
    );
    makeModules("examples_src_scala_com_pants_example_hello_exe_exe");

    assertNotNull(
      findClassFile("com.pants.example.hello.welcome.WelcomeEverybody", "examples_src_scala_com_pants_example_hello_welcome_welcome")
    );
    assertGotoFileContains("README");
    //Assert if README file under a sub directory is indexed.
    assertGotoFileContains("README_DOCS");
  }

  public void testExcludes1() throws Throwable {
    doImport("intellij-integration/src/scala/com/pants/testproject/excludes1");

    assertModules(
      "intellij-integration_src_scala_com_pants_testproject_excludes1_excludes1",
      "intellij-integration_src_scala_com_pants_testproject_excludes1_nested_foo_foo"
    );


    assertModuleModuleDeps(
      "intellij-integration_src_scala_com_pants_testproject_excludes1_excludes1",
      "intellij-integration_src_scala_com_pants_testproject_excludes1_nested_foo_foo"
    );
    makeModules("intellij-integration_src_scala_com_pants_testproject_excludes1_excludes1");

    assertNotNull(findClass("com.pants.testproject.excludes1.nested.foo.Foo"));

    assertNotNull(
      findClassFile(
        "com.pants.testproject.excludes1.nested.foo.Foo",
        "intellij-integration_src_scala_com_pants_testproject_excludes1_nested_foo_foo"
      )
    );
  }
}
