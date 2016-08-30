// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.testFramework.PantsTestUtils;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


public class WholeRepoIntegrationTest extends OSSPantsIntegrationTest {

  //@Override
  //public void setUp() throws Exception {
  //  super.setUp();
  //}
  //
  @NotNull
  @Override
  protected File getProjectFolder() {
    final String dummyRepoHome = System.getenv("DUMMY_REPO_HOME");
    assertNotNull(dummyRepoHome);
    return new File(dummyRepoHome);
  }

  @Override
  protected void setUpInWriteAction() throws Exception {
    super.passthruSetUpInWriteAction();
    myProjectRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(getProjectFolder());
    assertNotNull(myProjectRoot);

    cleanProjectRoot();

    //final Sdk sdk = JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
    //ProjectRootManager.getInstance(myProject).setProjectSdk(sdk);

  }

  public void testWholeRepo() throws Throwable {
    //doImport("examples::");
    doImport("");
    assertProjectName("dummy_repo/.::");
  }
}
