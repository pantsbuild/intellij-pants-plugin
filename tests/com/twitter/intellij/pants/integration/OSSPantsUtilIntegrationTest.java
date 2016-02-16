// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;

public class OSSPantsUtilIntegrationTest extends OSSPantsIntegrationTest {
  public void testOptionSupport() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/dummies/");
    String pantsExecutable = PantsUtil.findPantsExecutable(getProjectFolder().getPath()).getPath();
    assertTrue("No pants executable found", pantsExecutable != null);

    assertTrue(PantsUtil.supportsGoalOption(pantsExecutable, "test.junit", "--test-junit-fail-fast"));
    assertFalse(PantsUtil.supportsGoalOption(pantsExecutable, "export", "--some-invalid-flag"));
  }
}
