// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.importing;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.test.ExternalSystemImportingTestCase;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;

public abstract class PantsImportingTestCase extends ExternalSystemImportingTestCase {
  private PantsProjectSettings myProjectSettings;
  private String myRelativeProjectPath;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myProjectSettings = new PantsProjectSettings();
    myProjectSettings.setAllTargets(true);
  }

  @Override
  protected void setUpInWriteAction() throws Exception {
    super.setUpInWriteAction();
    final File projectTemplateFolder = getProjectFolderToCopy();
    if (!projectTemplateFolder.exists() || !projectTemplateFolder.isDirectory()) {
      fail("invalid template project path " + projectTemplateFolder.getAbsolutePath());
    }
    FileUtil.copyDirContent(projectTemplateFolder, new File(myProjectRoot.getPath()));
  }

  abstract protected File getProjectFolderToCopy();

  @Override
  protected String getProjectPath() {
    return super.getProjectPath() + "/" + StringUtil.notNullize(myRelativeProjectPath);
  }

  protected void doTest(String projectFolderPathToImport) {
    myRelativeProjectPath = projectFolderPathToImport;
    importProject();
  }

  @Override
  public void tearDown() throws Exception {
    try {
      Messages.setTestDialog(TestDialog.DEFAULT);
    }
    finally {
      super.tearDown();
    }
  }

  @Override
  protected void importProject(@NonNls @Language("Python") String config) throws IOException {
    super.importProject(config);
  }

  @Override
  protected ExternalProjectSettings getCurrentExternalProjectSettings() {
    return myProjectSettings;
  }

  @Override
  protected ProjectSystemId getExternalSystemId() {
    return PantsConstants.SYSTEM_ID;
  }

  @Override
  protected String getTestsTempDir() {
    return "pants";
  }

  @Override
  protected String getExternalSystemConfigFileName() {
    return "BUILD";
  }
}
