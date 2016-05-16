// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.intellij.openapi.project.DumbServiceImpl;
import com.intellij.openapi.project.Project;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class PantsMetrics {
  //private static PantsMetrics myMetrics = new PantsMetrics();
  private static final MetricRegistry metrics = new MetricRegistry();


  private static ScheduledFuture handle;

  private static final ScheduledExecutorService indexThreadPool = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "Pants-Plugin-Index-Pool");
      }
    });

  private static int counter = 0;
  public static void timeNextIndexing(Project myProject) {
    handle = indexThreadPool.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        // Still in smart mode, meaning indexing hasn't started yet.
        counter++;
        if (counter > 10) {
          handle.cancel(true);
          counter = 0;
        }
        if (!DumbServiceImpl.getInstance(myProject).isDumb()) {
          return;
        }
        Timer myTimer = metrics.timer("Indexing");
        final Timer.Context context = myTimer.time();
        DumbServiceImpl.getInstance(myProject).runWhenSmart(new Runnable() {
          @Override
          public void run() {
            context.stop();
            ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
              .convertRatesTo(TimeUnit.SECONDS)
              .convertDurationsTo(TimeUnit.MILLISECONDS)
              .build();
            reporter.report();
            //context.close();
          }
        });
        handle.cancel(false);
      }
    }, 0, 1, TimeUnit.SECONDS);
  }

  private static Timer resolveTimer = metrics.timer("resolve");
  private static Timer.Context resolveContext;
  public static void markResolveStart() {
    resolveContext = resolveTimer.time();
  }

  public static void markResolveEnd() {
    resolveContext.stop();
    ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
      .convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS)
      .build();
    reporter.report();
    //resolveContext.close();
  }
}
