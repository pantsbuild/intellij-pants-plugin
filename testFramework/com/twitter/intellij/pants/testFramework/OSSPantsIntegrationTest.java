// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

abstract public class OSSPantsIntegrationTest extends PantsIntegrationTestCase {
  public OSSPantsIntegrationTest() {
  }

  public OSSPantsIntegrationTest(boolean readOnly) {
    super(readOnly);
  }

  @NotNull
  @Override
  protected List<File> getProjectFoldersToCopy() {
    final File testProjects = new File(PantsTestUtils.findTestPath("testData"), "testprojects");
    return Collections.singletonList(testProjects);
  }

  private List<BeforeRunTask<?>> getBeforeRunTask(RunConfiguration configuration) {
    RunManagerImpl runManager = (RunManagerImpl) RunManager.getInstance(myProject);
    RunnerAndConfigurationSettingsImpl configurationSettings = new RunnerAndConfigurationSettingsImpl(runManager, configuration, true);
    runManager.addConfiguration(configurationSettings, true);
    List<BeforeRunTask<?>> tasks = runManager.getBeforeRunTasks(configuration);
    runManager.removeConfiguration(configurationSettings);
    return tasks;
  }

  /**
   * Assert `configuration` contains no before-run task such as Make or PantsMakeBeforeRun.
   *
   * @param configuration to add to the project.
   */
  protected void assertEmptyBeforeRunTask(RunConfiguration configuration) {
    assertEmpty(getBeforeRunTask(configuration));
  }

  /**
   * Assert Project has the right JDK and language level (JVM project only).
   */
  protected void assertProjectJdkAndLanguageLevel() {
    final String pantsExecutablePath = PantsUtil.findPantsExecutable(getProjectPath()).get().getPath();
    assertEquals(
      ProjectRootManager.getInstance(myProject).getProjectSdk().getHomePath(),
      getDefaultJavaSdk(pantsExecutablePath).get().getHomePath()
    );

    LanguageLevel projectLanguageLevel = LanguageLevelProjectExtension.getInstance(myProject).getLanguageLevel();
    LanguageLevel expectedLanguageLevel = LanguageLevel.JDK_1_8;
    assertTrue(
      String.format("Project Language Level should be %s, but is %s", expectedLanguageLevel, projectLanguageLevel),
      projectLanguageLevel.equals(LanguageLevel.JDK_1_8)
    );
  }

  @NotNull
  protected Document getTestData(String testDataPath) {
    File dataFile = PantsTestUtils.findTestPath(testDataPath);
    VirtualFile dataVirtualFile = LocalFileSystem.getInstance().findFileByPath(dataFile.getPath());
    assertNotNull(dataVirtualFile);
    Document dataDocument = FileDocumentManager.getInstance().getDocument(dataVirtualFile);
    assertNotNull(dataDocument);
    return dataDocument;
  }

  /**
   * Find document in project by filename.
   */
  @NotNull
  protected Document getDocumentFileInProject(String filename) {
    VirtualFile sourceFile = searchForVirtualFileInProject(filename);
    Document doc = FileDocumentManager.getInstance().getDocument(sourceFile);
    assertNotNull(String.format("%s not found.", filename), doc);
    return doc;
  }

  /**
   * Find VirtualFile in project by filename.
   */
  @NotNull
  protected VirtualFile searchForVirtualFileInProject(String filename) {
    Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(myProject, filename, GlobalSearchScope.allScope(myProject));
    assertEquals(String.format("%s not found.", filename), 1, files.size());
    return files.iterator().next();
  }

  /**
   * Find VirtualFile in project by filename.
   */
  @NotNull
  protected VirtualFile firstMatchingVirtualFileInProject(String filename) {
    Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(myProject, filename, GlobalSearchScope.allScope(myProject));
    assertTrue(String.format("Filename %s not found in project", filename), files.size() > 0);
    return files.iterator().next();
  }
}
