// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.testFramework.OSSPantsImportIntegrationTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PantsUtilTest extends OSSPantsImportIntegrationTest {

  public void testIsPantsProjectFile() {
    // Current project path should be under a Pants repo.
    assertTrue(PantsUtil.isPantsProjectFile(LocalFileSystem.getInstance().findFileByPath(getProjectPath())));
    // File system root should not.
    assertFalse(PantsUtil.isPantsProjectFile(LocalFileSystem.getInstance().findFileByPath("/")));
  }

  protected List<Sdk> getSameJdks(Sdk sdk) {
    return getAllJdks()
      .filter(jdk -> jdk == sdk)
      .collect(Collectors.toList());
  }

  public void testIsCompatibleVersion() {
    assertTrue(PantsUtil.isCompatibleVersion("2.2.3", "1.2.3"));
    assertTrue(PantsUtil.isCompatibleVersion("1.4.4", "1.2.3"));
    assertTrue(PantsUtil.isCompatibleVersion("1.2.4", "1.2.3"));
    assertTrue(PantsUtil.isCompatibleVersion("1.2.4rc0", "1.2.3"));
    assertTrue(PantsUtil.isCompatibleVersion("1.2.4.dev0", "1.2.3"));
    assertTrue(PantsUtil.isCompatibleVersion("1.2.3", "1.2.3"));

    assertFalse(PantsUtil.isCompatibleVersion("1.2.0rc122", "1.2.3"));
    assertFalse(PantsUtil.isCompatibleVersion("2.34.43", "2.34.44"));
    assertFalse(PantsUtil.isCompatibleVersion("2.33.44", "2.34.44"));
    assertFalse(PantsUtil.isCompatibleVersion("1.34.44", "2.34.44"));
  }

  public void testFindJdk() {
    final File executable = PantsUtil.findPantsExecutable(getProjectFolder()).get();
    assertEquals(Lists.newArrayList(), getAllJdks().collect(Collectors.toList()));

    final Sdk sdkA = getDefaultJavaSdk(executable.getPath()).get();
    assertEquals(Lists.newArrayList(sdkA), getSameJdks(sdkA));

    final List<Sdk> singleSdkInTable = getSameJdks(sdkA);
    assertTrue(singleSdkInTable.get(0).getName().contains("pants"));

    final List<Sdk> twoEntriesSameSdk = Lists.newArrayList(sdkA, sdkA);
    // manually adding the same jdk to the table should result in two identical
    // entries
    ApplicationManager.getApplication().runWriteAction(() -> {
        // no need to use disposable here, because this should not add a new jdk
        ProjectJdkTable.getInstance().addJdk(sdkA);
    });
    assertEquals(twoEntriesSameSdk, getSameJdks(sdkA));

    // calling getDefaultJavaSdk should only add a new entry to the table if it
    // needs to make one
    final Sdk sdkB = getDefaultJavaSdk(executable.getPath()).get();
    // Make sure they are identical, meaning that no new JDK was created on the 2nd find.
    assertTrue(sdkA == sdkB);
    assertEquals(twoEntriesSameSdk, getSameJdks(sdkA));
  }

  public void testRecreatesJdkIfRuntimeIsMissing() throws IOException {
    final File executable = PantsUtil.findPantsExecutable(getProjectFolder()).get();

    Sdk staleJdk = createStaleJdk(executable);
    assertEmpty(classesOf(staleJdk));

    Sdk recreatedJdk = getDefaultJavaSdk(executable.getPath()).get();
    Optional<VirtualFile> runtime = classesOf(recreatedJdk).stream()
      .filter(file -> file.getName().equals("rt.jar"))
      .findFirst();
    assertTrue(runtime.isPresent());

    HashSet<Sdk> allJdks = Sets.newHashSet(ProjectJdkTable.getInstance().getAllJdks());
    assertTrue(allJdks.contains(recreatedJdk)); // recreated jdk gets added
    assertFalse(allJdks.contains(staleJdk)); // stale Jdk gets removed
  }

  private Sdk createStaleJdk(File pantsExecutable) throws IOException {
    String name = String.format(PantsUtil.JDK_NAME_FORMAT, "1.8", pantsExecutable.getPath());
    Path jdkHome = Files.createTempDirectory("stale-jdk");
    return PantsUtil.registerNewJdk(name, jdkHome.toString(), getTestRootDisposable());
  }

  private Set<VirtualFile> classesOf(Sdk sdk) {
    return Sets.newHashSet(sdk.getRootProvider().getFiles(OrderRootType.CLASSES));
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
