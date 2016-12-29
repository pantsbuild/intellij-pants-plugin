// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.metrics;

import com.intellij.openapi.extensions.ExtensionPointName;

/**
 * This class keeps track of the metrics globally.
 * TODO: keep the metrics per project.
 */
public interface PantsExternalMetricsListener {

  public enum TestRunnerType {
    PANTS_RUNNER, JUNIT_RUNNER, SCALA_RUNNER
  }

  ExtensionPointName<PantsExternalMetricsListener>
    EP_NAME = ExtensionPointName.create("com.intellij.plugins.pants.pantsExternalMetricsListener");

  /**
   * @param isIncremental true if it is incremental import, otherwise it is the full graph import.
   */
  void logIncrementalImport(boolean isIncremental);

  /**
   * @param isGUI: true if it is GUI import, otherwise it is triggered from command line.
   */
  void logGUIImport(boolean isGUI);

  void logTestRunner(TestRunnerType runner);
}
