// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.google.common.collect.Lists;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.testFramework.OSSPantsImportIntegrationTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PantsUtilTest extends OSSPantsImportIntegrationTest {

  public void testIsPantsProjectFile() {
    // Current project path should be under a Pants repo.
    assertTrue(PantsUtil.isPantsProjectFile(LocalFileSystem.getInstance().findFileByPath(getProjectPath())));
    // File system root should not.
    assertFalse(PantsUtil.isPantsProjectFile(LocalFileSystem.getInstance().findFileByPath("/")));
  }

  public void testFindJdk() {
    final File executable = PantsUtil.findPantsExecutable(getProjectFolder()).get();
    final Sdk sdkA = getDefaultJavaSdk(executable.getPath()).get();
    List<Sdk> sdkResults = Arrays.stream(ProjectJdkTable.getInstance().getAllJdks())
      .filter(jdk -> jdk == sdkA)
      .collect(Collectors.toList());
    assertTrue(sdkResults.size() == 1);
    assertTrue(sdkResults.get(0) == sdkA);
    ApplicationManager.getApplication().runWriteAction(() -> {
        // no need to use disposable here, because this should not add a new jdk
        ProjectJdkTable.getInstance().addJdk(sdkA);
    });
    sdkResults = Arrays.stream(ProjectJdkTable.getInstance().getAllJdks())
      .filter(jdk -> jdk == sdkA)
      .collect(Collectors.toList());
    assertTrue(sdkResults.size() == 2);
    Sdk sdkB = getDefaultJavaSdk(executable.getPath()).get();
    // Make sure they are identical, meaning that no new JDK was created on the 2nd find.
    assertTrue(sdkA == sdkB);
    sdkResults = Arrays.stream(ProjectJdkTable.getInstance().getAllJdks())
      .filter(jdk -> jdk == sdkA)
      .collect(Collectors.toList());
    assertTrue(sdkResults.size() == 2);
    assertTrue(sdkResults.get(0) == sdkA);
  }

  public void testisBUILDFilePath() {
    assertFalse("pants.ini file should not be interpreted as a BUILD file",
                PantsUtil.isBUILDFilePath(pantsIniFilePath));

    assertFalse("made up file path should not be interpreted as a BUILD file",
                PantsUtil.isBUILDFilePath(nonexistentFilePath));

    assertTrue("made up BUILD file path should be interpreted as a BUILD file path",
               PantsUtil.isBUILDFilePath(nonexistentBuildFilePath));

    assertTrue("path to invalid, existing BUILD file should be interpreted as a BUILD file path",
               PantsUtil.isBUILDFilePath(invalidBuildFilePath));
  }

  public void testListAllTargets() {
    assertEquals("pants.ini file should have no targets",
                 PantsUtil.listAllTargets(pantsIniFilePath),
                 Lists.newArrayList());

    assertEquals("made up non-BUILD file path should have no targets",
                 PantsUtil.listAllTargets(nonexistentFilePath),
                 Lists.newArrayList());

    try {
      PantsUtil.listAllTargets(nonexistentBuildFilePath);
      fail(String.format("%s should have been thrown", PantsException.class));
    } catch (PantsException ignored) {
    }

    try {
      PantsUtil.listAllTargets(invalidBuildFilePath);
      fail(String.format("%s should have been thrown", PantsException.class));
    } catch (PantsException ignored) {
    }
  }
}
