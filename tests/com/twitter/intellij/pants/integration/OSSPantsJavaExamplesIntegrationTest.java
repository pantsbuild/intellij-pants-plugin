// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class OSSPantsJavaExamplesIntegrationTest extends OSSPantsIntegrationTest {
  public void testAnnotation() throws Throwable {
    doImport("examples/src/java/com/pants/examples/annotation/main");

    assertModules(
      "examples_src_java_com_pants_examples_annotation_example_example",
      "examples_src_java_com_pants_examples_annotation_main_main",
      "examples_src_java_com_pants_examples_annotation_processor_processor",
      "3rdparty_guava"
    );

    makeModules("examples_src_java_com_pants_examples_annotation_main_main");
    assertNotNull(
      findClassFile("com.pants.examples.annotation.main.Main", "examples_src_java_com_pants_examples_annotation_main_main")
    );
    assertNotNull(
      findClassFile("com.pants.examples.annotation.example.Example", "examples_src_java_com_pants_examples_annotation_example_example")
    );
    assertNotNull(
      findClassFile("com.pants.examples.annotation.processor.ExampleProcessor", "examples_src_java_com_pants_examples_annotation_processor_processor")
    );
  }

  public void testAntl3() throws Throwable {
    doImport("examples/src/java/com/pants/examples/antlr3");

    assertModules(
      "examples_src_java_com_pants_examples_antlr3_antlr3",
      "examples_src_antlr_com_pants_examples_exp_exp_antlr3",
      "_antlr-3.4",
      ".pants.d_gen_antlr_antlr3_gen-java_examples_src_antlr_examples.src.antlr.com.pants.examples.exp.exp_antlr3"
    );

    makeModules("examples_src_java_com_pants_examples_antlr3_antlr3");
    assertNotNull(
      findClassFile("com.pants.examples.antlr3.ExampleAntlr3", "examples_src_java_com_pants_examples_antlr3_antlr3")
    );
  }

  public void testAntl4() throws Throwable {
    doImport("examples/src/java/com/pants/examples/antlr4");

    assertModules(
      "examples_src_java_com_pants_examples_antlr4_antlr4",
      "examples_src_antlr_com_pants_examples_exp_exp_antlr4",
      "_antlr-4",
      ".pants.d_gen_antlr_antlr4_gen-java_examples_src_antlr_examples.src.antlr.com.pants.examples.exp.exp_antlr4"
    );

    makeModules("examples_src_java_com_pants_examples_antlr4_antlr4");
    assertNotNull(
      findClassFile("com.pants.examples.antlr4.ExampleAntlr4", "examples_src_java_com_pants_examples_antlr4_antlr4")
    );
  }

  public void testHello() throws Throwable {
    doImport("examples/src/java/com/pants/examples/hello");

    assertModules(
      "examples_src_resources_com_pants_example_hello_hello",
      "examples_src_java_com_pants_examples_hello_main_main",
      "examples_src_java_com_pants_examples_hello_greet_greet",
      "examples_src_java_com_pants_examples_hello_main_main-bin",
      "examples_src_java_com_pants_examples_hello_module"
    );

    makeModules("examples_src_java_com_pants_examples_hello_main_main");
    assertNotNull(
      findClassFile("com.pants.examples.hello.greet.Greeting", "examples_src_java_com_pants_examples_hello_greet_greet")
    );
    assertNotNull(
      findClassFile("com.pants.examples.hello.main.HelloMain", "examples_src_java_com_pants_examples_hello_main_main-bin")
    );
  }

  public void testJaxb() throws Throwable {
    doImport("examples/src/java/com/pants/examples/jaxb/main");

    assertModules(
      "examples_src_resources_com_pants_example_jaxb_jaxb",
      "examples_src_resources_com_pants_example_names_names",
      "examples_src_java_com_pants_examples_jaxb_main_main",
      "examples_src_java_com_pants_examples_jaxb_reader_reader",
      ".pants.d_gen_jaxb_gen-java_examples.src.resources.com.pants.example.jaxb.jaxb"
    );

    makeModules("examples_src_java_com_pants_examples_jaxb_main_main");
    assertNotNull(
      findClassFile("com.pants.examples.jaxb.main.ExampleJaxb", "examples_src_java_com_pants_examples_jaxb_main_main")
    );
  }

  public void testProtobuf() throws Throwable {
    doImport("examples/src/java/com/pants/examples/protobuf/distance");

    assertModules(
      "examples_src_java_com_pants_examples_protobuf_distance_distance",
      "examples_src_protobuf_com_pants_examples_distance_distance",
      "3rdparty_protobuf-java",
      ".pants.d_gen_protoc_gen-java_examples.src.protobuf.com.pants.examples.distance.distance"
    );

    makeModules("examples_src_java_com_pants_examples_protobuf_distance_distance");
    assertNotNull(
      findClassFile("com.pants.examples.protobuf.distance.ExampleProtobuf", "examples_src_java_com_pants_examples_protobuf_distance_distance")
    );
  }

  public void testExcludes1() throws Throwable {
    doImport("intellij-integration/src/java/com/pants/testproject/excludes1");

    assertModules(
      "intellij-integration_src_java_com_pants_testproject_excludes1_excludes1"
    );

    makeModules("intellij-integration_src_java_com_pants_testproject_excludes1_excludes1");
    assertNotNull(
      findClassFile(
        "com.pants.testproject.excludes1.Foo",
        "intellij-integration_src_java_com_pants_testproject_excludes1_excludes1"
      )
    );
  }

  public void testExcludes2() throws Throwable {
    doImport("intellij-integration/src/java/com/pants/testproject/excludes2");

    assertModules(
      "intellij-integration_src_java_com_pants_testproject_excludes2_excludes2",
      "intellij-integration_src_java_com_pants_testproject_excludes2_module"
    );

    makeModules("intellij-integration_src_java_com_pants_testproject_excludes2_excludes2");
    assertNotNull(
      findClassFile(
        "com.pants.testproject.excludes2.foo.Foo",
        "intellij-integration_src_java_com_pants_testproject_excludes2_excludes2"
      )
    );
  }
}
