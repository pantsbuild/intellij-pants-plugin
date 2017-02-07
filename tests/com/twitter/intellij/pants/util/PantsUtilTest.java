// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

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
    Optional<Sdk> sdk_a = PantsUtil.getDefaultJavaSdk(executable.get().getPath());
    Optional<Sdk> sdk_b = PantsUtil.getDefaultJavaSdk(executable.get().getPath());
    // Make sure they are identical
    assertTrue(sdk_a.get() == sdk_b.get());
  }
}
