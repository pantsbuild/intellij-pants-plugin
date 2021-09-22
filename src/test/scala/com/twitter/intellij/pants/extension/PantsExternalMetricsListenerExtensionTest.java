// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.extension;

import com.twitter.intellij.pants.metrics.PantsExternalMetricsListener;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

abstract public class PantsExternalMetricsListenerExtensionTest extends OSSPantsIntegrationTest {

  /**
   * Empty listener class, so the tests covering part of the `PantsExternalMetricsListener` do not have
   * to have all the boilerplate to implement all the abstract methods.
   */
  static class EmptyMetricsTestListener implements PantsExternalMetricsListener {

    @Override
    public void logIsIncrementalImport(boolean isIncremental) throws Throwable {
    }

    @Override
    public void logIsPantsNoopCompile(boolean isNoop) throws Throwable {
    }

    @Override
    public void logIsGUIImport(boolean isGUI) throws Throwable {
    }

    @Override
    public void logTestRunner(TestRunnerType runner) throws Throwable {
    }

    @Override
    public void logDurationBeforePantsCompile(long milliSeconds) throws Throwable {
    }

    @Override
    public void logIndexingDuration(long milliSeconds) throws Throwable {
    }

    @Override
    public void logEvent(String event) {
    }
  }


  @Override
  public void tearDown() throws Exception {
    gitResetRepoCleanExampleDistDir();
    super.tearDown();
  }

}
