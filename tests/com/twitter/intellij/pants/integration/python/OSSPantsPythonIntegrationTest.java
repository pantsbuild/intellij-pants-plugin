// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.python;

import com.intellij.util.ArrayUtil;
import com.intellij.util.PlatformUtils;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class OSSPantsPythonIntegrationTest extends OSSPantsIntegrationTest {
  @Override
  protected boolean shouldRunTest() {
    return super.shouldRunTest() && PlatformUtils.isCommunityEdition();
  }

  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "PythonCore");
  }

  public void testIntelliJIntegration() throws Throwable {
    final String pythonScript = "build-support/pants-intellij.sh";
    if (myProjectRoot.findFileByRelativePath(pythonScript) == null) {
      return;
    }
    doImport(pythonScript);

    assertNotNull(getModule("python_src"));
    assertNotNull(getModule("python_tests"));
    assertNotNull(getModule("python_requirements"));
  }
}
