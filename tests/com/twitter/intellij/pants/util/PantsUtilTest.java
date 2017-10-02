// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    // Make sure they are identical, meaning that no new JDK was created on the 2nd find.
    assertTrue(sdkA.get() == sdkB.get());
  }

  public void testListTargetsExistingNonBuildFile() {
    File projectDir = getProjectFolder();
    VirtualFile virtualProjectDir = LocalFileSystem.getInstance().findFileByIoFile(projectDir);
    Optional<VirtualFile> pantsIniFileResult = PantsUtil.findPantsIniFile(Optional.ofNullable(virtualProjectDir));
    assertTrue(pantsIniFileResult.isPresent());
    String pantsIniFilePath = pantsIniFileResult.get().getPath();
    File pantsIniFile = new File(pantsIniFilePath);
    assertTrue(pantsIniFile.exists() && !pantsIniFile.isDirectory());
    assertTrue(!PantsUtil.isBUILDFilePath(pantsIniFilePath));
    assertEquals(PantsUtil.listAllTargets(pantsIniFilePath),
                 Lists.newArrayList());
  }

  public void testListTargetsNonexistentNonBuildFile() {
    String nonexistentFilePath = "not/a/build/file/path";
    File nonexistentFile = new File(nonexistentFilePath);
    assertTrue(!nonexistentFile.exists());
    assertTrue(!PantsUtil.isBUILDFilePath(nonexistentFilePath));
    assertEquals(PantsUtil.listAllTargets(nonexistentFilePath),
                 Lists.newArrayList());
  }

  public void testListTargetsNonexistentBuildFile() {
    String nonexistentBuildFilePath = "not/a/real/BUILD";
    File nonexistentBuildFile = new File(nonexistentBuildFilePath);
    assertTrue(!nonexistentBuildFile.exists());
    assertTrue(PantsUtil.isBUILDFilePath(nonexistentBuildFilePath));
    boolean caught = false;
    try {
      PantsUtil.listAllTargets(nonexistentBuildFilePath);
    } catch (PantsException e) {
      caught = true;
    }
    assertTrue("PantsException not thrown for nonexistent BUILD file", caught);
  }

  public void testListTargetInvalidBuildFile() {
    String projectDir = getProjectFolder().getPath();
    Path invalidBuildFilePath = Paths.get(
      projectDir, "..", "invalid-build-file", "BUILD");
    String invalidBuildFileLocation = invalidBuildFilePath.normalize().toString();
    File invalidBuildFile = new File(invalidBuildFileLocation);
    assertTrue(invalidBuildFile.exists() && !invalidBuildFile.isDirectory());
    assertTrue(PantsUtil.isBUILDFilePath(invalidBuildFileLocation));
    boolean caught = false;
    try {
      PantsUtil.listAllTargets(invalidBuildFileLocation);
    } catch (PantsException e) {
      caught = true;
    }
    assertTrue("PantsException not thrown for invalid BUILD file", caught);
  }
}
