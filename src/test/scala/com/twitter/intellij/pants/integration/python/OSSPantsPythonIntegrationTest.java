// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.python;

import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTestWithPython;

import java.util.Arrays;
import java.util.List;

public class OSSPantsPythonIntegrationTest extends OSSPantsIntegrationTestWithPython {

  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "PythonCore");
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    List<String> pythonRoots = pythonRoots(Arrays.asList("src/python::", "tests/python/pants_test::", "contrib/::"));
    pythonRoots.add("/usr/local/Cellar");
    pythonRoots.add("/opt/python");
    pythonRoots.add("/usr/lib");

    // todo: Remove if possible. Now the test fails with VfsRootAccess to python interpreter in /opt
    VfsRootAccess.allowRootAccess(myProject, pythonRoots.toArray(new String[0]));
  }

  public void testIntelliJIntegration() {
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
