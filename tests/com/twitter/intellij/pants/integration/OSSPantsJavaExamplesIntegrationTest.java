// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingAnsiEscapesAwareProcessHandler;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.jps.incremental.ProjectBuildException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class OSSPantsJavaExamplesIntegrationTest extends OSSPantsIntegrationTest {
  public void testAnnotation() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/annotation/main");

    assertFirstSourcePartyModules(
      "examples_src_java_org_pantsbuild_example_annotation_example_example",
      "examples_src_java_org_pantsbuild_example_annotation_main_main",
      "examples_src_java_org_pantsbuild_example_annotation_processor_processor"
    );

    assertPantsCompileSuccess(pantsCompileModule("examples_src_java_org_pantsbuild_example_annotation_main_main"));
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

    assertFirstSourcePartyModules(
      "examples_src_java_org_pantsbuild_example_antlr3_antlr3",
      "examples_src_antlr_org_pantsbuild_example_exp_exp_antlr3"
    );
    assertGenModules(1);

    assertPantsCompileSuccess(pantsCompileModule("examples_src_java_org_pantsbuild_example_antlr3_antlr3"));
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.antlr3.ExampleAntlr3", "examples_src_java_org_pantsbuild_example_antlr3_antlr3"
    );
  }

  public void testAntl4() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/antlr4");

    assertFirstSourcePartyModules(
      "examples_src_java_org_pantsbuild_example_antlr4_antlr4",
      "examples_src_antlr_org_pantsbuild_example_exp_exp_antlr4"
    );
    assertGenModules(1);

    assertPantsCompileSuccess(pantsCompileModule("examples_src_java_org_pantsbuild_example_antlr4_antlr4"));
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.antlr4.ExampleAntlr4", "examples_src_java_org_pantsbuild_example_antlr4_antlr4"
    );
  }

  public void testHello() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/hello");

    String[] initialModules = {"examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_main_main",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module",
      "examples_src_java_org_pantsbuild_example_hello_main_readme",
      "examples_src_java_org_pantsbuild_example_hello_main_common_sources"};

    assertFirstSourcePartyModules(
      initialModules
    );

    assertPantsCompileSuccess(pantsCompileModule("examples_src_java_org_pantsbuild_example_hello_main_main"));
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.greet.Greeting", "examples_src_java_org_pantsbuild_example_hello_greet_greet"
    );
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.main.HelloMain", "examples_src_java_org_pantsbuild_example_hello_main_main-bin"
    );
  }

  public void testHelloWithDependees() throws Throwable {
    doImportWithDependees("examples/src/java/org/pantsbuild/example/hello");

    assertFirstSourcePartyModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome",  // direct dependee
      "examples_tests_java_org_pantsbuild_example_hello_greet_greet",     // direct dependee
      "examples_tests_scala_org_pantsbuild_example_hello_welcome_welcome",// transitive dependee
      "examples_src_scala_org_pantsbuild_example_jvm-run-example-lib",    // transitive dependee
      "examples_src_scala_org_pantsbuild_example_hello_hello",            // transitive dependee
      "examples_src_scala_org_pantsbuild_example_jvm-run-example",        // transitive dependee
      "examples_src_scala_org_pantsbuild_example_hello_exe_exe"           // transitive dependee
    );

    assertPantsCompileSuccess(pantsCompileProject());

    assertClassFileInModuleOutput(
      "org.pantsbuild.example.hello.greet.GreetingTest", "examples_tests_java_org_pantsbuild_example_hello_greet_greet"
    );
  }

  public void testJaxb() throws Throwable {
    String projectRelativePath = "examples/src/java/org/pantsbuild/example/jaxb/main";
    doImport(projectRelativePath);

    //Checking whether the modules loaded in the project are the same as pants dependencies
    String[] moduleNames = getModulesNamesFromPantsDependencies(projectRelativePath);
    assertTrue(moduleNames.length > 0);
    assertFirstSourcePartyModules(moduleNames);

    assertGenModules(1);

    assertPantsCompileSuccess(pantsCompileModule("examples_src_java_org_pantsbuild_example_jaxb_main_main"));
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.jaxb.main.ExampleJaxb", "examples_src_java_org_pantsbuild_example_jaxb_main_main"
    );
  }

  public void testProtobuf() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/protobuf/distance");

    assertFirstSourcePartyModules(
      "examples_src_java_org_pantsbuild_example_protobuf_distance_distance",
      "examples_src_protobuf_org_pantsbuild_example_distance_distance"
    );
    assertGenModules(1);

    assertPantsCompileSuccess(pantsCompileModule("examples_src_java_org_pantsbuild_example_protobuf_distance_distance"));
    assertClassFileInModuleOutput(
      "org.pantsbuild.example.protobuf.distance.ExampleProtobuf", "examples_src_java_org_pantsbuild_example_protobuf_distance_distance"
    );
  }

  public void testExcludes1() throws Throwable {
    doImport("intellij-integration/src/java/org/pantsbuild/testproject/excludes1");

    assertFirstSourcePartyModules(
      "intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1"
    );

    assertPantsCompileSuccess(
      pantsCompileModule("intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1")
    );
    assertClassFileInModuleOutput(
      "org.pantsbuild.testproject.excludes1.Foo", "intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1"
    );
  }

  public void testExcludes2() throws Throwable {
    doImport("intellij-integration/src/java/org/pantsbuild/testproject/excludes2");

    assertFirstSourcePartyModules(
      "intellij-integration_src_java_org_pantsbuild_testproject_excludes2_excludes2",
      "intellij-integration_src_java_org_pantsbuild_testproject_excludes2_module"
    );

    assertPantsCompileSuccess(
      pantsCompileModule("intellij-integration_src_java_org_pantsbuild_testproject_excludes2_excludes2")
    );
    assertClassFileInModuleOutput(
      "org.pantsbuild.testproject.excludes2.foo.Foo", "intellij-integration_src_java_org_pantsbuild_testproject_excludes2_excludes2"
    );
  }

  public void testResources1() throws Throwable {
    // test if we handle resources with '.' in path and don't override resources
    doImport("intellij-integration/src/java/org/pantsbuild/testproject/resources1");

    assertFirstSourcePartyModules(
      "intellij-integration_src_java_org_pantsbuild_testproject_resources1_resources1"
    );

    assertPantsCompileSuccess(
      pantsCompileModule("intellij-integration_src_java_org_pantsbuild_testproject_resources1_resources1")
    );
  }

  public void testCompileWithProjectJdk() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/hello/greet");

    assertFirstSourcePartyModules(
      "examples_src_java_org_pantsbuild_example_hello_greet_greet"
    );

    PantsSettings settings = PantsSettings.getInstance(myProject);
    settings.setUseIdeaProjectJdk(true);
    Pair<Boolean, Optional<String>> result = pantsCompileProject();
    assertPantsCompileSuccess(result);
    assertContainsSubstring(result.getSecond().get(), PantsConstants.PANTS_CLI_OPTION_JVM_DISTRIBUTIONS_PATHS);

    settings.setUseIdeaProjectJdk(false);
    Pair<Boolean, Optional<String>> resultB = pantsCompileProject();
    assertPantsCompileSuccess(result);
    assertNotContainsSubstring(resultB.getSecond().get(), PantsConstants.PANTS_CLI_OPTION_JVM_DISTRIBUTIONS_PATHS);
  }

  private String[] getModulesNamesFromPantsDependencies(String targetName) throws ProjectBuildException {
    Optional<VirtualFile>  pantsExe = PantsUtil.findPantsExecutable(myProject);
    assertTrue(pantsExe.isPresent());
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(pantsExe.get().getPath());
    commandLine.addParameters(PantsConstants.PANTS_CLI_OPTION_NO_COLORS);
    commandLine.addParameters("dependencies");
    commandLine.addParameters(targetName);
    final Process process;
    try {
      process = commandLine.createProcess();
    }
    catch (ExecutionException e) {
      throw new ProjectBuildException(e);
    }

    final CapturingProcessHandler processHandler = new CapturingAnsiEscapesAwareProcessHandler(process);
    ProcessOutput output = processHandler.runProcess();
    String lines[] = output.getStdout().split("\\r?\\n");
    Set<String> modules = new HashSet<String>();
    for (String l : lines) {
      modules.add(PantsUtil.getCanonicalModuleName(l));
    }
    return modules.toArray(new String[modules.size()]);
  }
}
