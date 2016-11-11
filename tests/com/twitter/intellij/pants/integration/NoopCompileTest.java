// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.testFramework.rules.TempDirectory;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.junit.Rule;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NoopCompileTest extends OSSPantsIntegrationTest {

  public NoopCompileTest(){
    // readOnly = false
    //super(false);
    super(true);
  }

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
    final File projectDir = new File(myProjectRoot.getPath());

    //cmd("git", "reset", "--hard");
    //cmd("git", "clean", "-fdx");
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

  //@Rule public TempDirectory myTempDir = new TempDirectory();
  //
  //public void testDummy() throws IOException {
  //  //TempDirectory myTempDir = new TempDirectory();
  //
  //  VirtualFile dir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(myTempDir.newFolder("vDir"));
  //  assertNotNull(dir);
  //  dir.getChildren();
  //
  //  Ref<Boolean> eventFired = Ref.create(false);
  //
  //  VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
  //    @Override
  //    public void fileCreated(@NotNull VirtualFileEvent event) {
  //      eventFired.set(true);
  //    }
  //  }, getTestRootDisposable());
  //
  //  new WriteAction() {
  //    @Override
  //    protected void run(@NotNull Result result) throws IOException {
  //      dir.createChildData(this, "x.txt");
  //    }
  //  }.execute();
  //
  //  assertTrue(eventFired.get());
  //}
}
