package com.twitter.intellij.pants.base;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.twitter.intellij.pants.file.BUILDFileTypeDetector;
import com.twitter.intellij.pants.file.PexFileTypeFactory;
import com.twitter.intellij.pants.inspections.PantsLibNotConfiguredInspection;
import com.twitter.intellij.pants.inspections.PantsLibNotFoundInspection;
import com.twitter.intellij.pants.util.PantsTestUtils;
import com.twitter.intellij.pants.util.PantsUtil;

abstract public class PantsCodeInsightFixtureTestCase extends LightCodeInsightFixtureTestCase {

  private String defaultUserHome = null;
  private BUILDFileTypeDetector buildFileTypeDetector = null;
  private PexFileTypeFactory pexFileTypeFactory = null;

  @Override
  protected String getTestDataPath() {
    return PantsTestUtils.BASE_TEST_DATA_PATH + getBasePath();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    defaultUserHome = System.getProperty("user.home");
    System.setProperty("user.home", FileUtil.toSystemIndependentName(PantsTestUtils.BASE_TEST_DATA_PATH + "/userHome"));

    buildFileTypeDetector = new BUILDFileTypeDetector();
    Extensions.getRootArea().getExtensionPoint(FileTypeRegistry.FileTypeDetector.EP_NAME).registerExtension(buildFileTypeDetector);

    pexFileTypeFactory = new PexFileTypeFactory();
    Extensions.getRootArea().getExtensionPoint(FileTypeFactory.FILE_TYPE_FACTORY_EP).registerExtension(pexFileTypeFactory);

    myFixture.addFileToProject("pants.ini", "pants_version: 0.239");
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        PantsLibNotFoundInspection.InstallQuickFix.applyFix(myFixture.getProject());
        PantsLibNotConfiguredInspection.ConfigureLibFix.applyFix(myFixture.getProject(), myFixture.getModule());
      }
    });
    assertNotNull(
      "Pants lib not configured!",
      ProjectLibraryTable.getInstance(myFixture.getProject()).getLibraryByName(PantsUtil.PANTS_LIBRAY_NAME)
    );
  }

  @Override
  protected void tearDown() throws Exception {
    System.setProperty("user.home", defaultUserHome);
    defaultUserHome = null;

    Extensions.getRootArea().getExtensionPoint(FileTypeRegistry.FileTypeDetector.EP_NAME).unregisterExtension(buildFileTypeDetector);
    buildFileTypeDetector = null;

    Extensions.getRootArea().getExtensionPoint(FileTypeFactory.FILE_TYPE_FACTORY_EP).unregisterExtension(pexFileTypeFactory);
    pexFileTypeFactory = null;

    final LibraryTable libraryTable = ProjectLibraryTable.getInstance(myFixture.getProject());
    final Library libraryByName = libraryTable.getLibraryByName(PantsUtil.PANTS_LIBRAY_NAME);
    if (libraryByName != null) {
      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          libraryTable.removeLibrary(libraryByName);
        }
      });
    }
    super.tearDown();
  }
}
