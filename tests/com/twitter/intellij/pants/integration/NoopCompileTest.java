// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class NoopCompileTest extends OSSPantsIntegrationTest {

  private static final String HELLO_SRC_JAVA_MODULE = "examples_src_java_org_pantsbuild_example_hello_greet_greet";
  private static final String HELLO_SRC_SCALA_MODULE = "examples_src_scala_org_pantsbuild_example_hello_welcome_welcome";
  private static final String HELLO_TEST_MODULE = "examples_tests_scala_org_pantsbuild_example_hello_welcome_welcome";
  private static final String HELLO_RESOURCES_MODULE = "examples_src_resources_org_pantsbuild_example_hello_hello";

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
    gitResetRepoCleanExampleDistDir();
    super.tearDown();
  }

  public void testNoop() throws Throwable {
    // The first compile has to execute.
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    // Second compile without any change should be noop.
    assertPantsCompileNoop(pantsCompileProject());
  }

  public void testEditFileInProjectShouldOp() throws Throwable {
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    modify("org.pantsbuild.example.hello.welcome.WelSpec");
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
  }

  public void testEditDocInProjectShouldOp() throws Throwable {
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    assertPantsCompileNoop(pantsCompileProject());
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        Document doc = getDocumentFileInProject("WelSpec.scala");
        doc.setText(doc.getText() + " ");
      }
    });
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    assertPantsCompileNoop(pantsCompileProject());
  }

  public void testAddFileInsideProjectShouldOp() throws Throwable {
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    // Simulate out of band adding a file outside of IDE
    Path newFilePath = Paths.get(getProjectFolder().getPath(), "examples/tests/scala/org/pantsbuild/example/hello/welcome/a.txt");
    Files.write(newFilePath, Collections.singleton("123"), Charset.defaultCharset());
    // When user switches back to IntelliJ LocalFileSystem refresh will be called.
    LocalFileSystem.getInstance().refresh(false);
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
  }

  public void testAddThenDeleteSameFileInsideProjectShouldNoop() throws Throwable {
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    // Simulate out of band adding a file outside of IDE
    Path newFilePath = Paths.get(getProjectFolder().getPath(), "examples/tests/scala/org/pantsbuild/example/hello/welcome/a.txt");
    Files.write(newFilePath, Collections.singleton("123"), Charset.defaultCharset());
    Files.delete(newFilePath);
    // When user switches back to IntelliJ LocalFileSystem refresh will be called.
    LocalFileSystem.getInstance().refresh(false);
    assertPantsCompileNoop(pantsCompileProject());
  }



  /**
   * NOTE: Disabled because it is flaky.
   * Also either behavior is okay in this case, although preferably noop.
   */
  //public void testAddThenDeleteFileOutsideProjectShouldNoop() throws Throwable {
  //  assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
  //  // Simulate out of band adding a file outside of IDE
  //  Path newFilePath = Paths.get(getProjectFolder().getPath(), "examples/a.txt");
  //  Files.write(newFilePath, Collections.singleton("123"), Charset.defaultCharset());
  //  // When user switches back to IntelliJ LocalFileSystem refresh will be called.
  //  LocalFileSystem.getInstance().refresh(false);
  //  assertPantsCompileNoop(pantsCompileProject());
  //
  //  Files.delete(newFilePath);
  //  LocalFileSystem.getInstance().refresh(false);
  //  assertPantsCompileNoop(pantsCompileProject());
  //}

  public void testAddThenDeleteFileInProjectShouldOp() throws Throwable {
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

  public void testShouldCompileAfterCleanAll() throws Throwable {
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
    assertPantsCompileNoop(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
    cmd("./pants", "clean-all");
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
  }

  public void testShouldCompileAfterOutOfBandPantsCLI() throws Throwable {
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
    assertPantsCompileNoop(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
    cmd("./pants", "export-classpath", "--manifest-jar-only", "examples/tests/java/org/pantsbuild/example/hello/greet");
    // Recompile because the sha of manifest.jar will change.
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule(HELLO_SRC_JAVA_MODULE));
  }
}
