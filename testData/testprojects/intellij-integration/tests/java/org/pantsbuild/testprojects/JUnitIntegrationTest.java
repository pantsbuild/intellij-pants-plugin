// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

// Test Hello World example's greet lib, which says "Hello" to things.

package org.pantsbuild.testprojects;

import org.junit.Test;

import static org.junit.Assert.*;

public class JUnitIntegrationTest {
  @Test
  public void alwaysWorks() {
    assertTrue(true);
  }

  @Test
  public void mightFail() {
    assertEquals("false", System.getProperty("PANTS_FAIL_TEST", "false"));
  }
}
