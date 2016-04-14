// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.twitter.intellij.pants.util.PantsConstants;
import junit.framework.TestCase;


public class PantsOptionsTest extends TestCase {

  public void testWorkdir() {
    PantsOptions options = new PantsOptions("pants_workdir = /Users/abc/workspace/intellij-pants-plugin/.pants.d (from HARDCODED)");
    assertEquals("/Users/abc/workspace/intellij-pants-plugin/.pants.d",
                 options.get(PantsConstants.PANTS_OPTION_PANTS_WORKDIR));
  }

  public void testWorkdirWithSpace() {
    PantsOptions options = new PantsOptions("pants_workdir = /Users/abc/workspace/intellij-pants-plugin/.pants.d (from HARDCODED)");
    assertEquals("/Users/abc/workspace/intellij-pants-plugin/.pants.d",
                 options.get(PantsConstants.PANTS_OPTION_PANTS_WORKDIR));
  }

  public void testInvalidWorkdir() {
    PantsOptions options = new PantsOptions("/Users/abc/workspace/intellij-pants-plugin/.pants.d (from HARDCODED)");
    assertNull(options.get(PantsConstants.PANTS_OPTION_PANTS_WORKDIR));
  }

  public void testStrictJvmVersion() {
    PantsOptions options = new PantsOptions("test.junit.strict_jvm_version = False (from HARDCODED)");
    assertEquals("False", options.get(PantsConstants.PANTS_OPTION_TEST_JUNIT_STRICT_JVM_VERSION));
  }
}
