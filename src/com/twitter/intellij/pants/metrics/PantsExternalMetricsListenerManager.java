// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.metrics;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.twitter.intellij.pants.util.PantsUtil;

import java.util.Arrays;


public class PantsExternalMetricsListenerManager implements PantsExternalMetricsListener {

  private static PantsExternalMetricsListenerManager instance = new PantsExternalMetricsListenerManager();

  public enum TestRunnerType {
    PANTS_RUNNER, JUNIT_RUNNER, SCALA_RUNNER
  }

  private static ExtensionPointName<PantsExternalMetricsListener>
    EP_NAME = ExtensionPointName.create("com.intellij.plugins.pants.pantsExternalMetricsListener");

  public static PantsExternalMetricsListenerManager getInstance() {
    return instance;
  }

  @Override
  public void logIncrementalImport(boolean isIncremental) {
    Arrays.stream(EP_NAME.getExtensions()).forEach(s -> s.logIncrementalImport(isIncremental));
  }

  @Override
  public void logGUIImport(boolean isGUI) {
    Arrays.stream(EP_NAME.getExtensions()).forEach(s -> s.logGUIImport(isGUI));
  }

  @Override
  public void logTestRunner(PantsExternalMetricsListener.TestRunner runner) {
    Arrays.stream(EP_NAME.getExtensions()).forEach(s -> s.logTestRunner(runner));
  }

  public void logTestRunner(RunConfiguration runConfiguration) {
    PantsUtil.RunConfigurationDecider.decideAndDo(
      runConfiguration,
      () -> Arrays.stream(EP_NAME.getExtensions()).forEach(s -> s.logTestRunner(PantsExternalMetricsListener.TestRunner.SCALA)),
      () -> Arrays.stream(EP_NAME.getExtensions()).forEach(s -> s.logTestRunner(PantsExternalMetricsListener.TestRunner.JUNIT))
    );
  }
}
