// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.metrics;

import com.google.common.base.Stopwatch;
import com.intellij.openapi.project.DumbService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class captures the indexing duration on the fly, which is independent from the pipeline where
 * pants export, project loading, and indexing happen in sequence.
 * <p>
 * My current understanding about how indexing works, aka project being dumb, is that multiple
 * components such as python and scala plugins can submit tasks to the framework, from which time to they
 * are completed, the project is in dumb mode. Also, indexing time can happen in any time and order.
 * <p>
 * For example:
 * <p>
 * When a project opens, both python and scala plugins found that some .py and .scala files have been modified,
 * they may submit jobs to update the indexing caches simultaneously. Assuming each job will take 5 seconds and there is
 * only a single CPU, `PantsDumbModeListener` will report 10 seconds.
 * <p>
 * In some other circumstances where files in IDE are not in sync with their counterparts on the disk, there
 * can be a delay where python plugin detects changes first, and one moment later, scala plugin does the same.
 * In this case, it is possible `PantsDumbModeListener` will report 5 seconds twice and separately.
 */
public class LivePantsMetrics implements DumbService.DumbModeListener {
  private final Stopwatch indexWatch = Stopwatch.createUnstarted();
  private final AtomicInteger count = new AtomicInteger(0);

  @Override
  public void enteredDumbMode() {
    if (count.getAndIncrement() == 0) {
      indexWatch.start();
    }
  }

  @Override
  public void exitDumbMode() {
    if (count.decrementAndGet() == 0) {
      indexWatch.stop();
      PantsExternalMetricsListenerManager.getInstance().logIndexingDuration(indexWatch.elapsed(TimeUnit.MILLISECONDS));
      indexWatch.reset();
    }
  }
}