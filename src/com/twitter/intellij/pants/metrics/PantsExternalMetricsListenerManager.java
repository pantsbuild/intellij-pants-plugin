// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.metrics;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.twitter.intellij.pants.util.PantsUtil;

import java.util.Arrays;


public class PantsExternalMetricsListenerManager implements PantsExternalMetricsListener {

  private static PantsExternalMetricsListenerManager instance = new PantsExternalMetricsListenerManager();

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
  public void logTestRunner(PantsExternalMetricsListener.TestRunnerType runner) {
    Arrays.stream(EP_NAME.getExtensions()).forEach(s -> s.logTestRunner(runner));
  }

  public void logTestRunner(RunConfiguration runConfiguration) {
    PantsUtil.RunConfigurationDecider.decideAndDo(
      runConfiguration,
      () -> Arrays.stream(EP_NAME.getExtensions()).forEach(s -> s.logTestRunner(TestRunnerType.SCALA_RUNNER)),
      () -> Arrays.stream(EP_NAME.getExtensions()).forEach(s -> s.logTestRunner(TestRunnerType.JUNIT_RUNNER))
    );
  }
}
