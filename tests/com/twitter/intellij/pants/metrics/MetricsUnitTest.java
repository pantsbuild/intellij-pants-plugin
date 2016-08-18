// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.metrics;

import com.twitter.intellij.pants.components.impl.PantsMetrics;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.util.Map;

public class MetricsUnitTest extends OSSPantsIntegrationTest {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    System.setProperty(PantsMetrics.SYSTEM_PROPERTY_METRICS_ENABLE, "true");
    PantsMetrics.initialize();
  }

  @Override
  public void tearDown() throws Exception {
    System.clearProperty(PantsMetrics.SYSTEM_PROPERTY_METRICS_ENABLE);
    super.tearDown();
  }

  public void testSanity() throws InterruptedException {
    PantsMetrics.markIndexStart();
    Thread.sleep(1000);
    PantsMetrics.markIndexEnd();
    Map<String, Long> result = PantsMetrics.getCurrentResult();
    assertTrue(0 == result.get("export_second"));
    assertTrue(0 == result.get("load_second"));
    assertTrue(0 < result.get("indexing_second"));
  }

  public void testMetricsEnabled() throws Exception {
    multipleCalls();
  }

  public void testMetricsDisabled() throws Exception {
    System.clearProperty(PantsMetrics.SYSTEM_PROPERTY_METRICS_ENABLE);
    multipleCalls();
  }

  /**
   * This test makes sure that PantsMetrics does not explode when called
   * from multiple places, in which case it does not provide actual useful information.
   *
   * In actual metrics test, these functions are called in order and in sync.
   *
   * @throws Exception
   */
  private void multipleCalls() throws Exception {
    PantsMetrics.markIndexStart();
    PantsMetrics.markIndexStart();
    PantsMetrics.markExportEnd();
    PantsMetrics.markExportEnd();
    PantsMetrics.markIndexStart();
    PantsMetrics.markIndexStart();
    PantsMetrics.markResolveStart();
    PantsMetrics.markResolveStart();
    PantsMetrics.markIndexStart();
    PantsMetrics.markResolveEnd();
    PantsMetrics.markResolveEnd();
    PantsMetrics.getCurrentResult();
    PantsMetrics.report();
  }
}
