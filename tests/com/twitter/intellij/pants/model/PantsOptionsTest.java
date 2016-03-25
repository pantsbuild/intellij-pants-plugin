// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import junit.framework.TestCase;

public class PantsOptionsTest extends TestCase {

  class PantsOptionsMock extends PantsOptions {
    PantsOptionsMock(String fixedRawContent) {
      super();
      rawData = fixedRawContent;
    }
  }

  public void testWorkdir() {
    PantsOptionsMock options = new PantsOptionsMock("pants_workdir = /Users/abc/workspace/intellij-pants-plugin/.pants.d (from HARDCODED)");
    assertEquals(options.getWorkdir(), "/Users/abc/workspace/intellij-pants-plugin/.pants.d");
  }

  public void testWorkdirWithSpace() {
    PantsOptionsMock options = new PantsOptionsMock("pants_workdir = /Users/abc/work space/intellij-pants-plugin/.pants.d (from HARDCODED)");
    assertEquals(options.getWorkdir(), "/Users/abc/work space/intellij-pants-plugin/.pants.d");
  }

  public void testInvalidWorkdir() {
    PantsOptionsMock options = new PantsOptionsMock("/Users/abc/work space/intellij-pants-plugin/.pants.d (from HARDCODED)");
    assertNull(options.getWorkdir());
  }
}
