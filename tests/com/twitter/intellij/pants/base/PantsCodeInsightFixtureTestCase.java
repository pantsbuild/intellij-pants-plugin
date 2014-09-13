package com.twitter.intellij.pants.base;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.twitter.intellij.pants.inspections.PantsLibNotConfiguredInspection;
import com.twitter.intellij.pants.inspections.PantsLibNotFoundInspection;
import com.twitter.intellij.pants.util.PantsTestUtils;
import com.twitter.intellij.pants.util.PantsUtil;

abstract public class PantsCodeInsightFixtureTestCase extends LightCodeInsightFixtureTestCase {
  private static final String PLUGINS_KEY = "idea.load.plugins.id";
  private static final String USER_HOME_KEY = "user.home";

  private String defaultUserHome = null;
  private String defaultPlugins = null;

  @Override
  protected String getTestDataPath() {
    return PantsTestUtils.BASE_TEST_DATA_PATH + getBasePath();
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
    assertTrue(
      "Python Community Edition plugin should be in classpath for tests\n" +
      "You need to include jars from ~/Library/Application Support/IdeaIC13/python/lib/",
      pyPlugin != null
    );

    checkDependentPlugins(pyPlugin);
    assertTrue(
      "Python Community Edition plugin should be enabled",
      pyPlugin.isEnabled()
    );

    myFixture.addFileToProject("pants.ini", "pants_version: 1.0.5rc2");
    ApplicationManager.getApplication().runWriteAction(
      new Runnable() {
        @Override
        public void run() {
          PantsLibNotFoundInspection.InstallQuickFix.applyFix(myFixture.getProject());
          PantsLibNotConfiguredInspection.ConfigureLibFix.applyFix(myFixture.getProject(), myFixture.getModule());
        }
      }
    );
    assertNotNull(
      "Pants lib not configured!",
      ProjectLibraryTable.getInstance(myFixture.getProject()).getLibraryByName(PantsUtil.PANTS_LIBRARY_NAME)
    );
  }

  private void checkDependentPlugins(IdeaPluginDescriptor mainPlugin) {
    for (PluginId pluginId : mainPlugin.getDependentPluginIds()) {
      if ("com.intellij.modules.java".equalsIgnoreCase(pluginId.getIdString())) {
        continue;
      }
      final IdeaPluginDescriptor plugin = PluginManager.getPlugin(pluginId);
      assertNotNull(
        pluginId.getIdString() + " plugin should be in classpath. " +
        mainPlugin.getPluginId().getIdString() + " needs it.",
        plugin
      );
      checkDependentPlugins(plugin);
    }
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
    final Library libraryByName = libraryTable.getLibraryByName(PantsUtil.PANTS_LIBRARY_NAME);
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
