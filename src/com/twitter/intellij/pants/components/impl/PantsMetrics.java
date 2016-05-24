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
import org.junit.Assert;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class PantsMetrics {
  private static MetricRegistry metricsRegistry = new MetricRegistry();

  private static ScheduledFuture handle;

  private static ScheduledExecutorService indexThreadPool;

  private static int counter = 0;

  private static String METRIC_INDEXING = "indexing_second";
  private static String METRIC_LOAD = "load_second";

  private static Timer.Context resolveContext;
  private static Timer.Context indexing_context;

  public static void timeNextIndexing(Project myProject) {
    Timer myTimer = metricsRegistry.timer(METRIC_INDEXING);
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      indexing_context = myTimer.time();
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
        indexing_context = myTimer.time();
        DumbServiceImpl.getInstance(myProject).runWhenSmart(new Runnable() {
          @Override
          public void run() {
            markIndexFinished();
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

  public static void markIndexFinished() {
    indexing_context.stop();
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
    String metricsDir = System.getProperty("metricsReportDir");
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
