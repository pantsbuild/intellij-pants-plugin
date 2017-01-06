// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.metrics;

import com.intellij.openapi.extensions.ExtensionPointName;

/**
 * This is the listener interface for related events happened in this plugin.
 * Other plugins can subscribe this interface via extension point system.
 */
public interface PantsExternalMetricsListener {

  enum TestRunnerType {
    PANTS_RUNNER, JUNIT_RUNNER, SCALA_RUNNER
  }

  ExtensionPointName<PantsExternalMetricsListener>
    EP_NAME = ExtensionPointName.create("com.intellij.plugins.pants.pantsExternalMetricsListener");

  /**
   * @param isIncremental true if it is incremental import, i.e. subgraph of a project,
   *                      otherwise it is the full graph import.
   */
  void logIsIncrementalImport(boolean isIncremental) throws Throwable;

  /**
   * @param isGUI: true if it is GUI import, otherwise it is triggered from command line.
   */
  void logIsGUIImport(boolean isGUI) throws Throwable;

  /**
   * Log the type of test runner invoked by user.
   *
   * @param runner TestRunnerType
   */
  void logTestRunner(TestRunnerType runner) throws Throwable;
}
