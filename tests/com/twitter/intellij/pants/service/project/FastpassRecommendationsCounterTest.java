// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FastpassRecommendationsCounterTest extends TestCase {
  static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public void testNeverFireForTheFirstTime() {
    FastpassRecommendationsCounter counter = new FastpassRecommendationsCounter();
    assertFalse(counter.tick(Duration.ZERO, dateOf("2020-06-09 16:00")));
  }

  public void testFireSecondTime() {
    FastpassRecommendationsCounter counter = new FastpassRecommendationsCounter();
    assertFalse(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:00")));
    assertTrue(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:05")));
  }

  public void testNoFireIfDistanceTooBig() {
    FastpassRecommendationsCounter counter = new FastpassRecommendationsCounter();
    assertFalse(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:00")));
    assertFalse(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:15")));
  }

  public void testNoFireIfBuildWasTooShort() {
    FastpassRecommendationsCounter counter = new FastpassRecommendationsCounter();
    assertFalse(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:00")));
    assertFalse(counter.tick(Duration.ofSeconds(5), dateOf("2020-06-09 16:15")));
  }

  public void testNoFireIfBuildWasTooLong() {
    FastpassRecommendationsCounter counter = new FastpassRecommendationsCounter();
    assertFalse(counter.tick(Duration.ofSeconds(61), dateOf("2020-06-09 16:00")));
    assertFalse(counter.tick(Duration.ofSeconds(5), dateOf("2020-06-09 16:15")));
  }

  public void testSkipIfFirstTickTooShort() {
    FastpassRecommendationsCounter counter = new FastpassRecommendationsCounter();
    assertFalse(counter.tick(Duration.ofSeconds(5), dateOf("2020-06-09 16:00")));
    assertFalse(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:04")));
    assertTrue(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:08")));
  }

  public void testSkipIfSecondTickTooShort() {
    FastpassRecommendationsCounter counter = new FastpassRecommendationsCounter();
    assertFalse(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:00")));
    assertFalse(counter.tick(Duration.ofSeconds(5), dateOf("2020-06-09 16:04")));
    assertTrue(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:08")));
  }

  public void testNoFireTwiceADay() {
    FastpassRecommendationsCounter counter = new FastpassRecommendationsCounter();
    assertFalse(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:00")));
    assertTrue(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:08")));
    assertFalse(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:09")));
  }

  public void testFireNextDay() {
    FastpassRecommendationsCounter counter = new FastpassRecommendationsCounter();
    assertFalse(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:00")));
    assertTrue(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-09 16:05")));
    assertFalse(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-10 16:00")));
    assertTrue(counter.tick(Duration.ofSeconds(20), dateOf("2020-06-10 16:05")));
  }

  @NotNull
  private LocalDateTime dateOf(String dateString) {
    return LocalDateTime.parse(dateString, formatter);
  }
}
