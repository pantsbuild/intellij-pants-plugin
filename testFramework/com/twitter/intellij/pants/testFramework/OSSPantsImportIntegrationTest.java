// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.testFramework.PantsTestUtils;
import com.twitter.intellij.pants.util.PantsUtil;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

abstract public class OSSPantsImportIntegrationTest extends OSSPantsIntegrationTest {

  protected String pantsIniFilePath;
  protected String nonexistentFilePath;
  protected String nonexistentBuildFilePath;
  protected String invalidBuildFilePath;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    VirtualFile virtualProjectDir = LocalFileSystem.getInstance().findFileByIoFile(getProjectFolder());
    this.pantsIniFilePath = PantsUtil.findPantsIniFile(Optional.ofNullable(virtualProjectDir)).get().getPath();

    this.nonexistentFilePath = "not/a/build-file/path";
    this.nonexistentBuildFilePath = "not/a/real/BUILD";

    Path invalidBuildFileLocation = Paths.get(
      PantsTestUtils.BASE_TEST_DATA_PATH, "invalid-build-file/BUILD");
    this.invalidBuildFilePath = invalidBuildFileLocation.normalize().toString();
  }

  public void verifyTestData() {
    File pantsIniFile = new File(pantsIniFilePath);
    assertTrue("pants.ini file should exist",
               pantsIniFile.exists());
    assertFalse("pants.ini file path should not resolve to a directory",
                pantsIniFile.isDirectory());

    File nonexistentFile = new File(nonexistentFilePath);
    assertFalse("made up file path should not exist", nonexistentFile.exists());

    File nonexistentBuildFile = new File(nonexistentBuildFilePath);
    assertFalse("made up BUILD file path should not exist",
                nonexistentBuildFile.exists());

    File invalidBuildFile = new File(invalidBuildFilePath);
    assertTrue("invalid BUILD file should exist", invalidBuildFile.exists());
    assertFalse("invalid BUILD file should not resolve to a directory",
                invalidBuildFile.isDirectory());
  }
}
