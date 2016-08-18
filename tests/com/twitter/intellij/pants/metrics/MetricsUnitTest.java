// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.metrics;

import com.twitter.intellij.pants.components.impl.PantsMetrics;
import junit.framework.TestCase;

import java.io.File;

public class MetricsUnitTest extends TestCase {
  @Override
  public void setUp() throws Exception {
    super.setUp();
    PantsMetrics.initialize();
  }

  public void testMetrics() throws Exception {
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
  }
}
