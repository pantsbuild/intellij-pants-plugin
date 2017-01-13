// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class PantsUtilTest extends OSSPantsIntegrationTest {

  public void testIsPantsProject() {
    // Current project path should be under a Pants repo.
    assertTrue(PantsUtil.isPantsProjectFile(LocalFileSystem.getInstance().findFileByPath(getProjectPath())));
    // File system root should not.
    assertFalse(PantsUtil.isPantsProjectFile(LocalFileSystem.getInstance().findFileByPath("/")));
  }
}
