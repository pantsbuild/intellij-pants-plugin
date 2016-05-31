// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbServiceImpl;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class PantsMetrics {
  private static final String SYSTEM_PROPERTY_METRICS_REPORT_DIR = "metrics.report.dir";
  private static final String SYSTEM_PROPERTY_METRICS_IMPORT_DIR = "metrics.import.dir";
  private static MetricRegistry metricsRegistry = new MetricRegistry();

  private static ScheduledFuture handle;

  private static ScheduledExecutorService indexThreadPool;

  private static int counter = 0;

  private static final String METRIC_INDEXING = "indexing_second";
  private static final String METRIC_LOAD = "load_second";
  private static final String METRIC_EXPORT = "export_second";

  private static Timer.Context resolveContext;
  private static Timer.Context indexingContext;
  private static Timer.Context exportContext;

  @Nullable
  public static String getMetricsImportDir() {
    return System.getProperty(SYSTEM_PROPERTY_METRICS_IMPORT_DIR);
  }

  public static void timeNextIndexing(Project myProject) {
    Timer myTimer = metricsRegistry.timer(METRIC_INDEXING);
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      indexingContext = myTimer.time();
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
        indexingContext = myTimer.time();
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
    if (indexThreadPool != null && !indexThreadPool.isShutdown()) {
      indexThreadPool.shutdown();
    }
    indexThreadPool = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
          return new Thread(r, "Pants-Plugin-Index-Pool");
        }
      });
  }

  public static void markResolveStart() {
    if (resolveContext != null) {
      resolveContext.close();
    }
    Timer resolveTimer = metricsRegistry.timer(METRIC_LOAD);
    resolveContext = resolveTimer.time();
  }

  public static void markResolveEnd() {
    resolveContext.stop();
    resolveContext.close();
  }

  public static void markExportStart() {
    if (exportContext != null) {
      exportContext.close();
    }
    Timer resolveTimer = metricsRegistry.timer(METRIC_EXPORT);
    exportContext = resolveTimer.time();
  }

  public static void markExportEnd() {
    exportContext.stop();
    exportContext.close();
  }

  public static void markIndexEnd() {
    indexingContext.stop();
  }

  public static void projectClosed() {
    if (handle != null) {
      handle.cancel(true);
    }
    if (indexThreadPool != null) {
      indexThreadPool.shutdown();
    }
    report();
  }

  public static void report() {
    String metricsDir = System.getProperty(SYSTEM_PROPERTY_METRICS_REPORT_DIR);
    // report csv if output dir specified.
    // otherwise report to console.
    if (metricsDir != null) {
      final CsvReporter csvReporter = CsvReporter.forRegistry(metricsRegistry)
        .formatFor(Locale.US)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.SECONDS)
        .build(new File(metricsDir));
      csvReporter.report();
      csvReporter.close();
    }
    else {
      ConsoleReporter reporter = ConsoleReporter.forRegistry(metricsRegistry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.SECONDS)
        .build();
      reporter.report();
      reporter.close();
    }
  }
}
