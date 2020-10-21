// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import java.time.Duration;
import java.time.LocalDateTime;

public class FastpassRecommendationsCounter {
  private LocalDateTime lastTick;
  private LocalDateTime lastNotify;

  public boolean tick(Duration buildLength, LocalDateTime time) {
    if(buildLength.getSeconds() >= 10 && buildLength.getSeconds() <= 60) {
      if(lastTick == null) {
        lastTick = time;
        return false;
      }
      Duration sinceLast = Duration.between(lastTick, time);
      lastTick = time;
      if(sinceLast.toMinutes() <= 60 &&
         (lastNotify == null ||lastNotify.getDayOfYear() != time.getDayOfYear())) {
        lastNotify = time;
        return true;
      }
    }
    return false;
  }
}
