// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbServiceImpl;
import com.intellij.openapi.project.Project;
import com.intellij.util.Time;
import com.sun.javafx.font.Metrics;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class PantsMetrics {
  //private static PantsMetrics myMetrics = new PantsMetrics();
  private static final MetricRegistry metricsRegistry = new MetricRegistry();


  private static ScheduledFuture handle;

  private static final ScheduledExecutorService indexThreadPool = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "Pants-Plugin-Index-Pool");
      }
    });

  private static int counter = 0;
  private static Timer.Context indexing_context;

  public static void timeNextIndexing(Project myProject) {
    handle = indexThreadPool.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        Timer myTimer = metricsRegistry.timer("indexing");

        if (ApplicationManager.getApplication().isUnitTestMode()) {
          indexing_context = myTimer.time();
          handle.cancel(false);
          return;
        }

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
          }
        });
        handle.cancel(false);
      }
    }, 0, 1, TimeUnit.SECONDS);
  }

  private static Timer resolveTimer = metricsRegistry.timer("resolve");
  private static Timer.Context resolveContext;

  public static void markResolveStart() {
    resolveContext = resolveTimer.time();
  }

  public static void markResolveEnd() {
    resolveContext.stop();
  }

  public static void markIndexFinished() {
    indexing_context.stop();
  }

  public static void projectClosed() {
    handle.cancel(true);
    indexThreadPool.shutdown();
    report();
  }

  public static void report() {
    //final CsvReporter csvReporter = CsvReporter.forRegistry(metricsRegistry)
    //  .formatFor(Locale.US)
    //  .convertRatesTo(TimeUnit.SECONDS)
    //  .convertDurationsTo(TimeUnit.SECONDS)
    //  .build(new File("/Users/yic/report/"));
    ////csvReporter.start(1, TimeUnit.SECONDS);    //resolveContext.close();
    ////csvReporter.start(0, TimeUnit.SECONDS);
    //csvReporter.report();
    //csvReporter.close();

    ConsoleReporter reporter = ConsoleReporter.forRegistry(metricsRegistry)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.SECONDS)
      .build();
    reporter.report();
    reporter.close();
  }
}
