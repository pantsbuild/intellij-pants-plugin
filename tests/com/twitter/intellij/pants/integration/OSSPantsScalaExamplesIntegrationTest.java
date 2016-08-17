// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.List;

public class OSSPantsScalaExamplesIntegrationTest extends OSSPantsIntegrationTest {
  public void testHelloByTargetName() throws Throwable {
    doImport("examples/src/scala/org/pantsbuild/example/hello/BUILD", "hello");

    assertSourceModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_scala_org_pantsbuild_example_hello_module",
      "examples_src_scala_org_pantsbuild_example_hello_hello",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_scala_org_pantsbuild_example_hello_exe_exe"
    );

    assertModuleModuleDeps(
      "examples_src_scala_org_pantsbuild_example_hello_exe_exe",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome"
    );
    assertPantsCompileModule("examples_src_scala_org_pantsbuild_example_hello_exe_exe");

    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.welcome.WelcomeEverybody", "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome"
    );
    assertGotoFileContains("README");
    //Assert if README file under a sub directory is indexed.
    assertGotoFileContains("README_DOCS");
  }

  public void testScalaWithJavaSources() throws Throwable {
    doImport("examples/src/scala/org/pantsbuild/example/scala_with_java_sources");

    final String moduleName =
      "examples_src_java_org_pantsbuild_example_java_sources_java_sources_and_scala_org_pantsbuild_example_scala_with_java_sources_scala_with_java_sources";

    assertSourceModules(moduleName);
    //makeProject();
    assertPantsCompileAll();

    assertClassFileInModuleOutput(
      "org.pantsbuild.example.scala_with_java_sources.GreetEverybody", moduleName
    );
  }

  public void testExcludes1() throws Throwable {
    doImport("intellij-integration/src/scala/org/pantsbuild/testproject/excludes1");

    assertSourceModules(
      "intellij-integration_src_scala_org_pantsbuild_testproject_excludes1_excludes1",
      "intellij-integration_src_scala_org_pantsbuild_testproject_excludes1_nested_foo_foo"
    );


    assertModuleModuleDeps(
      "intellij-integration_src_scala_org_pantsbuild_testproject_excludes1_excludes1",
      "___scala-library-synthetic",
      "intellij-integration_src_scala_org_pantsbuild_testproject_excludes1_nested_foo_foo"
    );
    assertPantsCompileModule("intellij-integration_src_scala_org_pantsbuild_testproject_excludes1_excludes1");

    findClassAndAssert("org.pantsbuild.testproject.excludes1.nested.foo.Foo");

    assertClassFileInModuleOutput(
      "org.pantsbuild.testproject.excludes1.nested.foo.Foo", "intellij-integration_src_scala_org_pantsbuild_testproject_excludes1_nested_foo_foo"
    );
  }

  public void testError1() throws Throwable {
    doImport("intellij-integration/src/scala/org/pantsbuild/testproject/error1");

    assertSourceModules(
      "intellij-integration_src_scala_org_pantsbuild_testproject_error1_error1"
    );
    assertPantsCompileModuleFailure("intellij-integration_src_scala_org_pantsbuild_testproject_error1_error1");
  }

  public void testWelcomeTest() throws Throwable {
    doImport("examples/tests/scala/org/pantsbuild/example/hello/welcome");

    assertSourceModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome",
      "examples_tests_scala_org_pantsbuild_example_hello_welcome_welcome"
    );

    final ContentEntry[] contentRoots = getContentRoots("examples_tests_scala_org_pantsbuild_example_hello_welcome_welcome");
    assertSize(1, contentRoots);
    final List<SourceFolder> testSourceRoots = contentRoots[0].getSourceFolders(JavaSourceRootType.TEST_SOURCE);
    assertSize(1, testSourceRoots);
    assertTrue(testSourceRoots.iterator().next().getUrl().endsWith("examples/tests/scala"));

    assertPantsCompileAll();

    findClassAndAssert("org.pantsbuild.example.hello.welcome.WelSpec");
    assertScalaLibrary("examples_tests_scala_org_pantsbuild_example_hello_welcome_welcome");

    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.welcome.WelSpec", "examples_tests_scala_org_pantsbuild_example_hello_welcome_welcome"
    );
  }
}
