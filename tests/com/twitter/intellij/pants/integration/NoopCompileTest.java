// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.util.ui.UIUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

public class NoopCompileTest extends OSSPantsIntegrationTest {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    doImport("examples/tests/scala/org/pantsbuild/example/hello/welcome");
    assertFirstSourcePartyModules(
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome",
      "examples_tests_scala_org_pantsbuild_example_hello_welcome_welcome"
    );
  }

  @Override
  public void tearDown() throws Exception {
    // Git reset .cache/pants dir
    cmd("git", "reset", "--hard");

    // Only the files under
    // examples/tests/scala/org/pantsbuild/example/hello/welcome/
    // examples/src/scala/org/pantsbuild/example/hello/welcome/
    // are going to be modified.
    // Hence issue `git clean -fdx` under examples
    File exampleDir = new File(getProjectFolder(), "examples");
    cmd(exampleDir, "git", "clean", "-fdx");
    super.tearDown();
  }

  public void testNoop() throws Throwable {
    assertPantsCompileSuccess(pantsCompileProject());
    assertPantsCompileNoop(pantsCompileProject());
  }

  public void testTouchFileShouldOp() throws Throwable {
    assertPantsCompileSuccess(pantsCompileProject());
    modify("org.pantsbuild.example.hello.welcome.WelSpec");
    assertPantsCompileSuccess(pantsCompileProject());
  }

  public void testAddFileShouldOp() throws Throwable {
    assertPantsCompileSuccess(pantsCompileProject());
    modify("org.pantsbuild.example.hello.welcome.WelSpec");
    File newFile = new File(getProjectFolder(), "examples/tests/scala/org/pantsbuild/example/hello/welcome/a.txt");
    Files.write(Paths.get(newFile.getPath()), Collections.singleton("123"), Charset.defaultCharset());
    UIUtil.dispatchAllInvocationEvents();
    assertPantsCompileSuccess(pantsCompileProject());
  }
}
