// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import org.jetbrains.annotations.NotNull;

import java.io.File;


public class WholeRepoIntegrationTest extends OSSPantsIntegrationTest {

  @NotNull
  @Override
  protected File getProjectFolder() {
    final String dummyRepoHome = System.getenv("DUMMY_REPO_HOME");
    assertNotNull(dummyRepoHome);
    return new File(dummyRepoHome);
  }

  @Override
  protected void setUpInWriteAction() throws Exception {
    super.passthroughSetUpInWriteAction();
    myProjectRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(getProjectFolder());
    assertNotNull(myProjectRoot);
  }

  public void testWholeRepo() throws Throwable {
    doImport("");
    assertProjectName("dummy_repo.::");
  }
}
