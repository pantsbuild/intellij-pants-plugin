// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.metrics;

import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class MetricsIntegrationTest extends OSSPantsIntegrationTest {
  public void testMetrics() throws Exception {
    String importDir = System.getProperty("metricsImportDir");
    doImport(importDir);
  }
}
