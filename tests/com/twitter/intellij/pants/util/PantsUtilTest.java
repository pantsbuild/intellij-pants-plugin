// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.io.File;
import java.util.Optional;

public class PantsUtilTest extends OSSPantsIntegrationTest {

  public void testIsPantsProjectFile() {
    // Current project path should be under a Pants repo.
    assertTrue(PantsUtil.isPantsProjectFile(LocalFileSystem.getInstance().findFileByPath(getProjectPath())));
    // File system root should not.
    assertFalse(PantsUtil.isPantsProjectFile(LocalFileSystem.getInstance().findFileByPath("/")));
  }

  public void testFindJdk() {
    Optional<File> executable = PantsUtil.findPantsExecutable(getProjectFolder());
    assertTrue(executable.isPresent());
    Optional<Sdk> sdkA = PantsUtil.getDefaultJavaSdk(executable.get().getPath());
    assertTrue(sdkA.isPresent());
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        ProjectJdkTable.getInstance().addJdk(sdkA.get());
      }
    });
    Optional<Sdk> sdkB = PantsUtil.getDefaultJavaSdk(executable.get().getPath());
    //Make sure they are identical.
    assertTrue(sdkA.get() == sdkB.get());
  }
}
