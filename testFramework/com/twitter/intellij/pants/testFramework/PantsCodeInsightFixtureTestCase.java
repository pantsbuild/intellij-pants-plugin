// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.twitter.intellij.pants.inspections.PantsLibNotConfiguredInspection;
import com.twitter.intellij.pants.inspections.PantsLibNotFoundInspection;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;

abstract public class PantsCodeInsightFixtureTestCase extends LightCodeInsightFixtureTestCase {
  private static final String PLUGINS_KEY = "idea.load.plugins.id";
  private static final String USER_HOME_KEY = "user.home";

  private String defaultUserHome = null;
  private String defaultPlugins = null;

  private final String myPath;

  public PantsCodeInsightFixtureTestCase() {
    myPath = "";
  }

  public PantsCodeInsightFixtureTestCase(String... path) {
    myPath = getPath(path);
  }

  private static String getPath(String... args) {
    final StringBuilder result = new StringBuilder();
    for (String folder : args) {
      result.append("/");
      result.append(folder);
    }
    return result.toString();
  }

  @Override
  protected String getTestDataPath() {
    return PantsTestUtils.BASE_TEST_DATA_PATH + getBasePath();
  }

  @Override
  protected String getBasePath() {
    return myPath;
  }

  @Override
  protected void setUp() throws Exception {
    final String pyPluginId = "PythonCore";

    defaultPlugins = System.getProperty(PLUGINS_KEY);
    System.setProperty(PLUGINS_KEY, pyPluginId + "," + defaultPlugins);

    defaultUserHome = System.getProperty(USER_HOME_KEY);
    System.setProperty(USER_HOME_KEY, FileUtil.toSystemIndependentName(PantsTestUtils.BASE_TEST_DATA_PATH + "/userHome"));

    super.setUp();

    final IdeaPluginDescriptor pyPlugin = PluginManager.getPlugin(PluginId.getId(pyPluginId));
    assertNotNull(
      "Python Community Edition plugin should be in classpath for tests\n" +
      "You need to include jars from ~/Library/Application Support/IdeaIC14/python/lib/",
      pyPlugin
    );

    if (!pyPlugin.isEnabled()) {
      assertTrue("Failed to enable Python plugin!", PluginManagerCore.enablePlugin(pyPluginId));
    }

    final VirtualFile folderWithPex = PantsUtil.findFolderWithPex();
    assertNotNull("Folder with pex files should be configured", folderWithPex);
    final VirtualFile[] pexFiles = folderWithPex.getChildren();
    assertTrue("There should be only one pex file!", pexFiles.length == 1);

    ApplicationManager.getApplication().runWriteAction(
      new Runnable() {
        @Override
        public void run() {
          PantsLibNotFoundInspection.InstallQuickFix.configureByFile(myFixture.getProject(), pexFiles[0]);
          PantsLibNotConfiguredInspection.ConfigureLibFix.applyFix(myFixture.getProject(), myFixture.getModule());
        }
      }
    );
    assertNotNull(
      "Pants lib not configured!",
      ProjectLibraryTable.getInstance(myFixture.getProject()).getLibraryByName(PantsConstants.PANTS_LIBRARY_NAME)
    );
  }

  protected void setUpPantsExecutable() {
    myFixture.addFileToProject(PantsConstants.PANTS, ""); // make it pants working dir
  }

  @Override
  protected void tearDown() throws Exception {
    if (defaultUserHome != null) {
      System.setProperty(USER_HOME_KEY, defaultUserHome);
      defaultUserHome = null;
    }
    if (defaultPlugins != null) {
      System.setProperty(PLUGINS_KEY, defaultPlugins);
      defaultPlugins = null;
    }

    final LibraryTable libraryTable = ProjectLibraryTable.getInstance(myFixture.getProject());
    final Library libraryByName = libraryTable.getLibraryByName(PantsConstants.PANTS_LIBRARY_NAME);
    if (libraryByName != null) {
      ApplicationManager.getApplication().runWriteAction(
        new Runnable() {
          @Override
          public void run() {
            libraryTable.removeLibrary(libraryByName);
          }
        }
      );
    }
    super.tearDown();
  }
}
