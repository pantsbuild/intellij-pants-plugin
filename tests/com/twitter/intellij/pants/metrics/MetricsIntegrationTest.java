// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.metrics;

import com.google.common.io.Files;
import com.google.gson.stream.JsonReader;
import com.intellij.openapi.util.io.FileUtil;
import com.twitter.intellij.pants.components.impl.PantsMetrics;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

public class MetricsIntegrationTest extends OSSPantsIntegrationTest {
  private File tempDir = null;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    System.setProperty(PantsMetrics.SYSTEM_PROPERTY_METRICS_ENABLE, "true");
    if (PantsMetrics.getMetricsImportDir() == null) {
      PantsMetrics.setMetricsImportDir("examples/src/java/org/pantsbuild/example/hello");
    }
    if (PantsMetrics.getMetricsReportDir() == null) {
      tempDir = Files.createTempDir();
      PantsMetrics.setMetricsReportDir(tempDir.getAbsolutePath());
    }
  }

  public void testMetrics() throws Exception {
    String importDir = PantsMetrics.getMetricsImportDir();
    assertNotNull(importDir);
    doImport(importDir);

    PantsMetrics.report();
    String reportFilePath = PantsMetrics.getReportFilePath();
    assertNotNull(reportFilePath);
    Map result = PantsUtil.gson.fromJson(new JsonReader(new FileReader(reportFilePath)), PantsUtil.TYPE_MAP_STRING_INTEGER);
    assertTrue(0 <= (int) result.get("export_second"));
    assertTrue(0 <= (int) result.get("load_second"));
    assertTrue(0 <= (int) result.get("indexing_second"));
  }

  @Override
  public void tearDown() throws Exception {
    System.clearProperty(PantsMetrics.SYSTEM_PROPERTY_METRICS_ENABLE);
    if (tempDir != null) {
      FileUtil.delete(tempDir);
    }
    super.tearDown();
  }
}
