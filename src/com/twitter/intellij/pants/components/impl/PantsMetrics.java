// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.google.common.base.Stopwatch;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbServiceImpl;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nullable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This class keeps track of the metrics globally.
 * TODO: keep the metrics per project.
 */
public class PantsMetrics {
  public static ScheduledExecutorService indexThreadPool;

  private static ConcurrentHashMap<String, Stopwatch> timers = new ConcurrentHashMap<>();
  public static final String SYSTEM_PROPERTY_METRICS_REPORT_DIR = "pants.metrics.report.dir";
  public static final String SYSTEM_PROPERTY_METRICS_IMPORT_DIR = "pants.metrics.import.dir";

  private static volatile ScheduledFuture handle;
  private static volatile int counter = 0;

  private static final String METRIC_INDEXING = "indexing_second";
  private static final String METRIC_LOAD = "load_second";
  private static final String METRIC_EXPORT = "export_second";


  @Nullable
  public static String getMetricsImportDir() {
    return System.getProperty(SYSTEM_PROPERTY_METRICS_IMPORT_DIR);
  }

  public static void setMetricsImportDir(String dir) {
    System.setProperty(SYSTEM_PROPERTY_METRICS_IMPORT_DIR, dir);
  }

  @Nullable
  public static String getMetricsReportDir() {
    return System.getProperty(SYSTEM_PROPERTY_METRICS_REPORT_DIR);
  }

  public static void setMetricsReportDir(String dir) {
    System.setProperty(SYSTEM_PROPERTY_METRICS_REPORT_DIR, dir);
  }

  /**
   * This starts the indexing timer when certain conditions are met,
   * because the factor to determine whether indexing has started is
   * different in unit test and in GUI mode.
   *
   * @param myProject current Project.
   */
  public static void prepareTimeIndexing(Project myProject) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      markIndexStart();
      return;
    }

    /**
     * This portion only applies in manual testing.
     */
    handle = indexThreadPool.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        // Start counting now in unit test mode, because dumb mode is never set.
        if (!DumbServiceImpl.getInstance(myProject).isDumb()) {
          return;
        }
        // Still in smart mode, meaning indexing hasn't started yet.
        counter++;
        if (counter > 10) {
          handle.cancel(false);
          counter = 0;
        }
        markIndexStart();
        DumbServiceImpl.getInstance(myProject).runWhenSmart(new Runnable() {
          @Override
          public void run() {
            markIndexEnd();
            report();
          }
        });
        handle.cancel(false);
      }
    }, 0, 1, TimeUnit.SECONDS);
  }

  public static void initialize() {
    timers.put(METRIC_EXPORT, Stopwatch.createUnstarted());
    timers.put(METRIC_LOAD, Stopwatch.createUnstarted());
    timers.put(METRIC_INDEXING, Stopwatch.createUnstarted());

    // Thread related things.
    if (indexThreadPool != null && !indexThreadPool.isShutdown()) {
      indexThreadPool.shutdown();
    }
    indexThreadPool = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
          return new Thread(r, "Pants-Plugin-Metrics-Pool");
        }
      });
  }

  public static void markResolveStart() {
    timers.get(METRIC_LOAD).start();
  }

  public static void markResolveEnd() {
    timers.get(METRIC_LOAD).stop();
  }

  public static void markExportStart() {
    timers.get(METRIC_EXPORT).start();
  }

  public static void markExportEnd() {
    timers.get(METRIC_EXPORT).stop();
  }

  public static void markIndexStart() {
    timers.get(METRIC_INDEXING).start();
  }

  public static void markIndexEnd() {
    timers.get(METRIC_INDEXING).stop();
  }

  public static void report() {
    Map<String, Long> report =
      timers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().elapsed(TimeUnit.SECONDS)));
    System.out.println(report);
    String reportFilePath = getReportFilePath();
    if (reportFilePath == null) {
      return;
    }
    try (Writer writer = new FileWriter(reportFilePath)) {
      PantsUtil.gson.toJson(report, writer);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Nullable
  public static String getReportFilePath() {
    String reportDir = getMetricsReportDir();
    if (reportDir == null) {
      return null;
    }
    return getMetricsReportDir() + "/output.json";
  }
}
