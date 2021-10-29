// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.extension;

import com.intellij.openapi.extensions.Extensions;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListener;

public class NonEdtPantsExternalMetricsListenerExtensionTest extends PantsExternalMetricsListenerExtensionTestBase {

  @Override
  protected boolean runInDispatchThread() {
    return false;
  }

  public void testJUnitRunner() throws Throwable {
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

    doImport("testprojects/tests/java/org/pantsbuild/testproject/annotation");
    assertPantsCompileExecutesAndSucceeds(pantsCompileProject());
    assertSuccessfulTest(
      "testprojects_tests_java_org_pantsbuild_testproject_annotation_annotation",
      "org.pantsbuild.testproject.annotation.AnnotationTest"
    );
    assertEquals(PantsExternalMetricsListener.TestRunnerType.JUNIT_RUNNER, listener.lastRun);
  }
}
