// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.util.List;

public class OSSPantsScalaExamplesIntegrationTest extends OSSPantsIntegrationTest {
  public void testHelloByTargetName() throws Throwable {
    doImport("examples/src/scala/com/pants/example/hello/BUILD", "hello");

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

    assertClassFileInModuleOutput(
      "com.pants.example.hello.welcome.WelcomeEverybody", "examples_src_scala_com_pants_example_hello_welcome_welcome"
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

    assertClassFileInModuleOutput(
      "com.pants.testproject.excludes1.nested.foo.Foo", "intellij-integration_src_scala_com_pants_testproject_excludes1_nested_foo_foo"
    );
  }

  public void testError1() throws Throwable {
    doImport("intellij-integration/src/scala/com/pants/testproject/error1");

    assertModules(
      "intellij-integration_src_scala_com_pants_testproject_error1_error1"
    );

    final List<CompilerMessage> compilerMessages =
      compileAndGetMessages(getModule("intellij-integration_src_scala_com_pants_testproject_error1_error1"));
    final List<String> errorMessages =
      ContainerUtil.mapNotNull(
        compilerMessages,
        new Function<CompilerMessage, String>() {
          @Override
          public String fun(CompilerMessage message) {
            if (message.getCategory() == CompilerMessageCategory.ERROR) {
              return message.getMessage();
            }
            return null;
          }
        }
      );
    assertNotEmpty(errorMessages);
    final boolean compileWithIntellij = PantsSettings.getInstance(myTestFixture.getProject()).isCompileWithIntellij();
    if (compileWithIntellij) {
      assertNotEmpty(errorMessages);
    } else {
      assertContainsElements(errorMessages, "pants: FAILURE");
    }
  }
}
