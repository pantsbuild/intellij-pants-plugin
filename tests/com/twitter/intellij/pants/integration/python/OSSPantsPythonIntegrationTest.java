// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.python;

import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class OSSPantsPythonIntegrationTest extends OSSPantsIntegrationTest {
  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "PythonCore");
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // todo: Remove if possible. Now the test fails with VfsRootAccess to python interpreter in /opt
    VfsRootAccess.allowRootAccess("/");
  }

  public void testIntelliJIntegration() throws Throwable {
    final String pythonScript = "build-support/pants-intellij.sh";
    if (myProjectRoot.findFileByRelativePath(pythonScript) == null) {
      return;
    }
    doImport(pythonScript);

    assertModuleExists("python_src");
    assertModuleExists("python_tests");
    assertModuleExists("python_requirements");
  }
}
