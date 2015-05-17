// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class OSSPantsJavaExamplesIntegrationTest extends OSSPantsIntegrationTest {
  public void testAnnotation() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/annotation/main");

    assertModules(
      "examples_src_java_org_pantsbuild_example_annotation_example_example",
      "examples_src_java_org_pantsbuild_example_annotation_main_main",
      "examples_src_java_org_pantsbuild_example_annotation_processor_processor"
    );

    makeModules("examples_src_java_org_pantsbuild_example_annotation_main_main");
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.annotation.main.Main", "examples_src_java_org_pantsbuild_example_annotation_main_main"
    );
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.annotation.example.Example", "examples_src_java_org_pantsbuild_example_annotation_example_example"
    );
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.annotation.processor.ExampleProcessor", "examples_src_java_org_pantsbuild_example_annotation_processor_processor"
    );
  }

  public void testAntl3() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/antlr3");

    assertModules(
      "examples_src_java_org_pantsbuild_example_antlr3_antlr3",
      "examples_src_antlr_org_pantsbuild_example_exp_exp_antlr3",
      ".pants.d_gen_antlr_antlr3_gen-java_examples_src_antlr_examples.src.antlr.org.pantsbuild.example.exp.exp_antlr3"
    );

    makeModules("examples_src_java_org_pantsbuild_example_antlr3_antlr3");
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.antlr3.ExampleAntlr3", "examples_src_java_org_pantsbuild_example_antlr3_antlr3"
    );
  }

  public void testAntl4() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/antlr4");

    assertModules(
      "examples_src_java_org_pantsbuild_example_antlr4_antlr4",
      "examples_src_antlr_org_pantsbuild_example_exp_exp_antlr4",
      ".pants.d_gen_antlr_antlr4_gen-java_examples_src_antlr_examples.src.antlr.org.pantsbuild.example.exp.exp_antlr4"
    );

    makeModules("examples_src_java_org_pantsbuild_example_antlr4_antlr4");
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.antlr4.ExampleAntlr4", "examples_src_java_org_pantsbuild_example_antlr4_antlr4"
    );
  }

  public void testHello() throws Throwable {
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
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.main.HelloMain", "examples_src_java_org_pantsbuild_example_hello_main_main-bin"
    );
  }

  public void testHelloWithDependees() throws Throwable {
    doImportWithDependees("examples/src/java/org/pantsbuild/example/hello");

    assertModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome", // dependee
      "examples_tests_java_org_pantsbuild_example_hello_greet_greet"     // dependee
    );

    makeProject();
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.greet.GreetingTest", "examples_tests_java_org_pantsbuild_example_hello_greet_greet"
    );
  }

  public void testJaxb() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/jaxb/main");

    assertModules(
      "examples_src_resources_org_pantsbuild_example_jaxb_jaxb",
      "examples_src_resources_org_pantsbuild_example_names_names",
      "examples_src_java_org_pantsbuild_example_jaxb_main_main",
      "examples_src_java_org_pantsbuild_example_jaxb_reader_reader",
      ".pants.d_gen_jaxb_gen-java_examples.src.resources.org.pantsbuild.example.jaxb.jaxb"
    );

    makeModules("examples_src_java_org_pantsbuild_example_jaxb_main_main");
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.jaxb.main.ExampleJaxb", "examples_src_java_org_pantsbuild_example_jaxb_main_main"
    );
  }

  public void testProtobuf() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/protobuf/distance");

    assertModules(
      "examples_src_java_org_pantsbuild_example_protobuf_distance_distance",
      "examples_src_protobuf_org_pantsbuild_example_distance_distance",
      ".pants.d_gen_protoc_gen-java_examples.src.protobuf.org.pantsbuild.example.distance.distance"
    );

    makeModules("examples_src_java_org_pantsbuild_example_protobuf_distance_distance");
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.protobuf.distance.ExampleProtobuf", "examples_src_java_org_pantsbuild_example_protobuf_distance_distance"
    );
  }

  public void testExcludes1() throws Throwable {
    doImport("intellij-integration/src/java/org/pantsbuild/testproject/excludes1");

    assertModules(
      "intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1"
    );

    makeModules("intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1");
    assertClassFileInModuleOutput(
      "org.pantsbuild.testproject.excludes1.Foo", "intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1"
    );
  }

  public void testExcludes2() throws Throwable {
    doImport("intellij-integration/src/java/org/pantsbuild/testproject/excludes2");

    assertModules(
      "intellij-integration_src_java_org_pantsbuild_testproject_excludes2_excludes2",
      "intellij-integration_src_java_org_pantsbuild_testproject_excludes2_module"
    );

    makeModules("intellij-integration_src_java_org_pantsbuild_testproject_excludes2_excludes2");
    assertClassFileInModuleOutput(
      "org.pantsbuild.testproject.excludes2.foo.Foo", "intellij-integration_src_java_org_pantsbuild_testproject_excludes2_excludes2"
    );
  }

  public void testResources1() throws Throwable {
    // test if we handle resources with '.' in path and don't override resources
    doImport("intellij-integration/src/java/org/pantsbuild/testproject/resources1");

    assertModules(
      "intellij-integration_src_java_org_pantsbuild_testproject_resources1_resources1"
    );

    makeModules("intellij-integration_src_java_org_pantsbuild_testproject_resources1_resources1");
  }
}
