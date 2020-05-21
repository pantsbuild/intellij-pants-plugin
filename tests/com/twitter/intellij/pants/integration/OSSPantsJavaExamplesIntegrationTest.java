// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingAnsiEscapesAwareProcessHandler;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.execution.PantsExecuteTaskResult;
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

    assertPantsCompileExecutesAndSucceeds(pantsCompileModule("examples_src_java_org_pantsbuild_example_annotation_main_main"));
  }

  public void testHello() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/hello");
    assertProjectJdkAndLanguageLevel();

    String[] initialModules = {
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_main_main",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module",
      "examples_src_java_org_pantsbuild_example_hello_main_readme",
      "examples_src_java_org_pantsbuild_example_hello_main_common_sources"
    };

    assertFirstSourcePartyModules(
      initialModules
    );

    assertPantsCompileExecutesAndSucceeds(pantsCompileModule("examples_src_java_org_pantsbuild_example_hello_main_main"));
  }

  public void testProtobuf() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/protobuf/distance");

    assertFirstSourcePartyModules(
      "examples_src_java_org_pantsbuild_example_protobuf_distance_distance",
      "examples_src_protobuf_org_pantsbuild_example_distance_distance"
    );
    assertGenModules(1);

    assertPantsCompileExecutesAndSucceeds(pantsCompileModule("examples_src_java_org_pantsbuild_example_protobuf_distance_distance"));
  }

  public void testExcludes1() throws Throwable {
    doImport("intellij-integration/src/java/org/pantsbuild/testproject/excludes1");

    assertFirstSourcePartyModules(
      "intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1"
    );

    assertPantsCompileExecutesAndSucceeds(
      pantsCompileModule("intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1")
    );
  }

  public void testExcludes2() throws Throwable {
    doImport("intellij-integration/src/java/org/pantsbuild/testproject/excludes2");

    assertFirstSourcePartyModules(
      "intellij-integration_src_java_org_pantsbuild_testproject_excludes2_excludes2",
      "intellij-integration_src_java_org_pantsbuild_testproject_excludes2_module"
    );

    assertPantsCompileExecutesAndSucceeds(
      pantsCompileModule("intellij-integration_src_java_org_pantsbuild_testproject_excludes2_excludes2")
    );
  }

  public void testResources1() throws Throwable {
    // test if we handle resources with '.' in path and don't override resources
    doImport("intellij-integration/src/java/org/pantsbuild/testproject/resources1");

    assertFirstSourcePartyModules(
      "intellij-integration_src_java_org_pantsbuild_testproject_resources1_resources1"
    );

    assertPantsCompileExecutesAndSucceeds(
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
    PantsExecuteTaskResult result = pantsCompileProject();
    assertPantsCompileExecutesAndSucceeds(result);
    assertContainsSubstring(result.output.get(), PantsConstants.PANTS_CLI_OPTION_JVM_DISTRIBUTIONS_PATHS);
    assertContainsSubstring(result.output.get(), PantsUtil.getJdkPathFromIntelliJCore());

    settings.setUseIdeaProjectJdk(false);
    PantsExecuteTaskResult resultB = pantsCompileProject();
    assertPantsCompileExecutesAndSucceeds(result);
    assertContainsSubstring(resultB.output.get(), PantsConstants.PANTS_CLI_OPTION_JVM_DISTRIBUTIONS_PATHS);
    assertContainsSubstring(resultB.output.get(), ProjectRootManager.getInstance(myProject).getProjectSdk().getHomePath());
  }

  private String[] getModulesNamesFromPantsDependencies(String targetName) throws ProjectBuildException {
    Optional<VirtualFile> pantsExe = PantsUtil.findPantsExecutable(myProject);
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

    final CapturingProcessHandler processHandler = new CapturingAnsiEscapesAwareProcessHandler(process, commandLine.getCommandLineString());
    ProcessOutput output = processHandler.runProcess();
    String lines[] = output.getStdout().split("\\r?\\n");
    Set<String> modules = new HashSet<>();
    for (String l : lines) {
      modules.add(PantsUtil.getCanonicalModuleName(l));
    }
    return modules.toArray(new String[modules.size()]);
  }
}
