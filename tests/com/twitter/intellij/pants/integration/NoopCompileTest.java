// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class NoopCompileTest extends OSSPantsIntegrationTest {

  public static final String HELLO_SRC_JAVA_MODULE = "examples_src_java_org_pantsbuild_example_hello_greet_greet";
  public static final String HELLO_SRC_SCALA_MODULE = "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome";
  public static final String HELLO_TEST_MODULE = "examples_tests_scala_org_pantsbuild_example_hello_welcome_welcome";
  public static final String HELLO_RESOURCES_MODULE = "examples_src_resources_org_pantsbuild_example_hello_hello";

  @Override
  public void setUp() throws Exception {
    super.setUp();
    doImport("examples/tests/scala/org/pantsbuild/example/hello/welcome");
    assertFirstSourcePartyModules(
      HELLO_RESOURCES_MODULE,
      HELLO_SRC_JAVA_MODULE,
      HELLO_SRC_SCALA_MODULE,
      HELLO_TEST_MODULE
    );
  }

  @Override
  public void tearDown() throws Exception {
    // Git reset .cache/pants dir
    cmd("git", "reset", "--hard");
    // Only the files under examples are going to be modified.
    // Hence issue `git clean -fdx` under examples, so pants does not
    // have to bootstrap again.
    File exampleDir = new File(getProjectFolder(), "examples");
    cmd(exampleDir, "git", "clean", "-fdx");
    super.tearDown();
  }

  public void testNoop() throws Throwable {
    // The first compile has to execute.
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    // Second compile without any change should be noop.
    assertPantsCompileNoop(pantsCompileProject());
  }

  public void testTouchFileShouldOp() throws Throwable {
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    modify("org.pantsbuild.example.hello.welcome.WelSpec");
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
  }

  public void testAddFileShouldOp() throws Throwable {
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    // Simulate out of band adding a file outside of IDE
    Path newFilePath = Paths.get(getProjectFolder().getPath(), "examples/tests/scala/org/pantsbuild/example/hello/welcome/a.txt");
    Files.write(newFilePath, Collections.singleton("123"), Charset.defaultCharset());
    // When user switches back to IntelliJ LocalFileSystem refresh will be called.
    LocalFileSystem.getInstance().refresh(false);
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
  }

  public void testAddThenDeleteFileShouldOp() throws Throwable {
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    // Simulate out of band adding a file outside of IDE
    Path newFilePath = Paths.get(getProjectFolder().getPath(), "examples/tests/scala/org/pantsbuild/example/hello/welcome/a.txt");
    Files.write(newFilePath, Collections.singleton("123"), Charset.defaultCharset());
    // When user switches back to IntelliJ, LocalFileSystem refresh will be called.
    LocalFileSystem.getInstance().refresh(false);
    Files.delete(newFilePath);
    LocalFileSystem.getInstance().refresh(false);
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
  }

  public void testCompileDifferentModule() throws Throwable {
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
    assertPantsCompileNoop(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
    // Compile a different module, should not noop.
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_SCALA_MODULE));
    assertPantsCompileNoop(pantsCompileModule(HELLO_SRC_SCALA_MODULE));
    // Switch back should compile again.
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
  }

  public void testCompileProjectSettings() throws Throwable {
    PantsSettings settings = PantsSettings.getInstance(myProject);
    settings.setUseIdeaProjectJdk(false);
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
    assertPantsCompileNoop(pantsCompileModule(HELLO_SRC_JAVA_MODULE));

    settings.setUseIdeaProjectJdk(true);
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
  }

  public void testCompileIncrementalImports() throws Throwable {
    // If a project uses incremental imports, then noop should never happen.
    PantsSettings settings = PantsSettings.getInstance(myProject);
    settings.setEnableIncrementalImport(true);
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
  }
}
