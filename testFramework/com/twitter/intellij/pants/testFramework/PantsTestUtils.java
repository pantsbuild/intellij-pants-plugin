// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import com.intellij.openapi.application.PathManager;

import java.io.File;

public class PantsTestUtils {
  /**
   * The root of the test data directory
   */
  public static final String BASE_TEST_DATA_PATH = findTestPath("testData").getAbsolutePath();

  public static File findTestPath(String folderName) {
    final File f = new File(folderName);
    if (f.exists()) {
      return f.getAbsoluteFile();
    }
    return new File(PathManager.getHomePath() + "/plugins/pants/" + folderName);
  }
}
