// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

public class PantsCompileOptionsExecutorTest extends OSSPantsIntegrationTest {

  public void testProjectName() throws Throwable {
    String deepDir = new String(new char[100]).replace("\0", "dummy/");
    assertTrue(deepDir.length() > PantsCompileOptionsExecutor.PROJECT_NAME_LIMIT);

    PantsExecutionSettings settings = new PantsExecutionSettings(
      Collections.singletonList(deepDir),
      false, // include libs and sources. does not matter here
      false, // use idea project jdk. does not matter here.
      false, // pants qexport dep as jar
      Optional.empty(), // incremental imports. does not matter here.
      false // use intellij compiler
    );

    PantsCompileOptionsExecutor executor = PantsCompileOptionsExecutor.create(
      getProjectFolder().getPath(),
      settings
    );

    String projectName = executor.getDefaultProjectName();
    assertNotContainsSubstring(projectName, File.separator);
    assertEquals(PantsCompileOptionsExecutor.PROJECT_NAME_LIMIT, projectName.length());
  }
}
