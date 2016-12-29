// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.extension;

import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.openapi.extensions.Extensions;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListener;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListenerManager;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import junit.framework.AssertionFailedError;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;

public class PantsExternalMetricsListenerExtensionTest extends OSSPantsIntegrationTest {

  private PantsExternalMetricsListener.TestRunnerType lastRun;


  public class DummyMetricsListener implements PantsExternalMetricsListener {

    @Override
    public void logIncrementalImport(boolean isIncremental) {

    }

    @Override
    public void logGUIImport(boolean isGUI) {

    }

    @Override
    public void logTestRunner(TestRunnerType runner) {
      lastRun = runner;
    }
  }


  @Override
  public void setUp() throws Exception {
    super.setUp();
    // Register `DummyMetricsListener` as one of the extension points of PantsExternalMetricsListener
    Extensions.getRootArea().getExtensionPoint(PantsExternalMetricsListener.EP_NAME).registerExtension(new DummyMetricsListener());
  }

  public void testLogTestRunner() {
    PantsExternalMetricsListenerManager.getInstance().logTestRunner(PantsExternalMetricsListener.TestRunnerType.JUNIT_RUNNER);
  }

  public void testJUnitRunner() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/annotation");

    JUnitConfiguration runConfiguration = generateJUnitConfiguration(
      "testprojects_tests_java_org_pantsbuild_testproject_annotation_annotation",
      "org.pantsbuild.testproject.annotation.AnnotationTest",
      null
    );

    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    assertSuccessfulTest(runConfiguration);
    assertEquals(PantsExternalMetricsListener.TestRunnerType.JUNIT_RUNNER, lastRun);
  }

  public void testScalaRunnerMetrics() {
    doImport("examples/tests/scala/org/pantsbuild/example/hello");
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    ScalaTestRunConfiguration runConfiguration = generateScalaRunConfiguration(
      "examples_tests_scala_org_pantsbuild_example_hello_welcome_welcome",
      "org.pantsbuild.example.hello.welcome.WelSpec",
      null
    );
    try {
      runWithConfiguration(runConfiguration);
    }
    catch (AssertionFailedError ignored) {

    }
    assertEquals(PantsExternalMetricsListener.TestRunnerType.SCALA_RUNNER, lastRun);
  }
}
