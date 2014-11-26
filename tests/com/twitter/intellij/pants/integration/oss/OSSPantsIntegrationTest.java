// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.oss;

import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.integration.base.PantsIntegrationTestCase;
import com.twitter.intellij.pants.util.PantsTestUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

abstract public class OSSPantsIntegrationTest extends PantsIntegrationTestCase {
  @Override
  protected List<File> getProjectFoldersToCopy() {
    final File ossFolder = getOSSFolder();
    final File testProjects = new File(PantsTestUtils.findTestPath("testData"), "testprojects");
    return Arrays.asList(ossFolder, testProjects);
  }

  protected File getOSSFolder() {
    final String ossPantsHome = System.getenv("OSS_PANTS_HOME");
    if (!StringUtil.isEmpty(ossPantsHome)) {
      return new File(ossPantsHome);
    }
    final File workingDir = PantsTestUtils.findTestPath("testData").getParentFile();
    return new File(workingDir.getParent(), "pants");
  }
}
