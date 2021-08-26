// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.extension;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.DumbServiceImpl;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListener;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListenerManager;
import junit.framework.AssertionFailedError;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;

public class EdtPantsExternalMetricsListenerExtensionTest extends PantsExternalMetricsListenerExtensionTest{

  public void testScalaRunnerMetrics() throws Exception {
    class TestMetricsListener extends EmptyMetricsTestListener {
      private PantsExternalMetricsListener.TestRunnerType lastRun;

      @Override
      public void logTestRunner(TestRunnerType runner) {
        lastRun = runner;
      }
    }

    // Register `TestMetricsListener` as one of the extension points of PantsExternalMetricsListener
    TestMetricsListener listener = new TestMetricsListener();
    Extensions.getRootArea().getExtensionPoint(PantsExternalMetricsListener.EP_NAME).registerExtension(listener, myProject);

    doImport("examples/tests/scala/org/pantsbuild/example/hello");
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    ScalaTestRunConfiguration runConfiguration = generateScalaRunConfiguration(
      "examples_tests_scala_org_pantsbuild_example_hello_welcome_welcome",
      "org.pantsbuild.example.hello.welcome.WelSpec"
    );
    try {
      runWithConfiguration(runConfiguration);
    }
    catch (AssertionFailedError ignored) {

    }
    assertEquals(PantsExternalMetricsListener.TestRunnerType.SCALA_RUNNER, listener.lastRun);
  }

  public void testJUnitRunnerError() throws Throwable {

    class ErrorMetricsListener extends EmptyMetricsTestListener {

      private boolean called = false;

      @Override
      public void logTestRunner(TestRunnerType runner) throws Throwable {
        called = true;
        throw new Exception("metrics error");
      }
    }

    ErrorMetricsListener errorListenerExtension = new ErrorMetricsListener();
    Extensions.getRootArea().getExtensionPoint(PantsExternalMetricsListener.EP_NAME).registerExtension(errorListenerExtension);

    // Make sure the exception will not cause the main thread to fail.
    PantsExternalMetricsListenerManager.getInstance().logTestRunner(PantsExternalMetricsListener.TestRunnerType.PANTS_RUNNER);
    assertTrue(String.format("%s was not called.", ErrorMetricsListener.class), errorListenerExtension.called);
  }

  public void testNoopMetrics() throws Throwable {
    class NoopMetricsListener extends EmptyMetricsTestListener {

      private boolean lastWasNoop;

      @Override
      public void logIsPantsNoopCompile(boolean isNoop) throws Throwable {
        lastWasNoop = isNoop;
      }
    }

    NoopMetricsListener listener = new NoopMetricsListener();
    Extensions.getRootArea().getExtensionPoint(PantsExternalMetricsListener.EP_NAME).registerExtension(listener);

    doImport("examples/tests/scala/org/pantsbuild/example/hello/welcome");
    // The first compile has to execute.
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    assertFalse("Last compile should not be noop, it was.", listener.lastWasNoop);

    // Second compile without any change should be lastWasNoop.
    assertPantsCompileNoop(pantsCompileProject());
    assertTrue("Last compile should be noop, but was not.", listener.lastWasNoop);
  }

  public void testDurationSinceLastEdit() throws Throwable {
    class DurationMetricsTestListener extends EmptyMetricsTestListener {
      private long duration = -1;

      @Override
      public void logDurationBeforePantsCompile(long milliSeconds) throws Throwable {
        duration = milliSeconds;
      }
    }

    DurationMetricsTestListener listener = new DurationMetricsTestListener();
    Extensions.getRootArea().getExtensionPoint(PantsExternalMetricsListener.EP_NAME).registerExtension(listener);

    doImport("examples/tests/scala/org/pantsbuild/example/hello/welcome");
    // The first compile has to execute.
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());

    for (long sleepMilliseconds : ContainerUtil.newArrayList(500, 1000)) {
      // Modify a file in project so PantsCompile will be triggered.
      modify("org.pantsbuild.example.hello.greet.Greeting");
      Thread.sleep(sleepMilliseconds);
      // Second compile with modified project should execute.
      assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
      assertTrue(
        "Recorded duration between last file edit and PantsCompile invocation should be refreshed, but it is not",
        listener.duration >= sleepMilliseconds
      );
      // Record the current duration.
      long dataPoint = listener.duration;
      // Run compile again which should be noop, and make sure the the duration is not updated.
      assertPantsCompileNoop(pantsCompileProject());
      assertEquals("Noop compile should leave recorded duration unchanged, but it is not the case", dataPoint, listener.duration);
    }
  }

  public void testDurationIndexing() throws Throwable {
    class DurationMetricsTestListener extends EmptyMetricsTestListener {
      private long duration = -1;

      @Override
      public void logIndexingDuration(long milliSeconds) throws Throwable {
        duration = milliSeconds;
      }
    }

    DurationMetricsTestListener listener = new DurationMetricsTestListener();
    Extensions.getRootArea().getExtensionPoint(PantsExternalMetricsListener.EP_NAME).registerExtension(listener);

    // Set DumbService manually to explicitly trigger the StopWatch for indexing.
    // The goal is to test the whether `logIndexingDuration` works.
    ApplicationManager.getApplication().invokeAndWait(
      () -> {
        DumbServiceImpl.getInstance(myProject).setDumb(true);
        // Set dumb mode multiple times to test the semaphore behavior.
        DumbServiceImpl.getInstance(myProject).setDumb(true);
      }
    );

    long sleepTimeMilliSeconds = 500;
    Thread.sleep(sleepTimeMilliSeconds);

    // Unset dumb service to signify indexing has ended.
    ApplicationManager.getApplication().invokeAndWait(() -> {
      DumbServiceImpl.getInstance(myProject).setDumb(false);
      DumbServiceImpl.getInstance(myProject).setDumb(false);
    });


    assertTrue(
      String.format("Indexing duration should be greater than %s, but is %s.", sleepTimeMilliSeconds, listener.duration),
      listener.duration >= sleepTimeMilliSeconds
    );

    // reset
    listener.duration = -1;

    // 2nd wave of indexing.
    ApplicationManager.getApplication().invokeAndWait(() -> DumbServiceImpl.getInstance(myProject).setDumb(true));


    long secondSleepTimeMilliSeconds = 1000;
    Thread.sleep(secondSleepTimeMilliSeconds);

    // Unset dumb service to signify indexing has ended.
    ApplicationManager.getApplication().invokeAndWait(() -> DumbServiceImpl.getInstance(myProject).setDumb(false));

    assertTrue(
      String.format("Indexing duration should be greater than %s, but is %s.", secondSleepTimeMilliSeconds, listener.duration),
      listener.duration >= secondSleepTimeMilliSeconds
    );
  }
}
