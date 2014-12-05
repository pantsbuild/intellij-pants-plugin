// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.oss;

import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.integration.base.PantsIntegrationTestCase;
import com.twitter.intellij.pants.util.PantsTestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;

abstract public class OSSPantsIntegrationTest extends PantsIntegrationTestCase {
  public OSSPantsIntegrationTest() {
  }

  public OSSPantsIntegrationTest(boolean needToCopyProjectToTempDir) {
    super(needToCopyProjectToTempDir);
  }

  @NotNull
  @Override
  protected List<File> getProjectFoldersToCopy() {
    final File testProjects = new File(PantsTestUtils.findTestPath("testData"), "testprojects");
    return Collections.singletonList(testProjects);
  }

  @Nullable
  @Override
  protected File getProjectFolder() {
    final String ossPantsHome = System.getenv("OSS_PANTS_HOME");
    if (!StringUtil.isEmpty(ossPantsHome)) {
      return new File(ossPantsHome);
    }
    final File workingDir = PantsTestUtils.findTestPath("testData").getParentFile();
    return new File(workingDir.getParent(), "pants");
  }
}
