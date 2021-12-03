// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsConstants;
import junit.framework.TestCase;

import java.util.Optional;


public class PantsOptionsTest extends BasePlatformTestCase {

  public void testWorkdir() {
    PantsOptions options = new PantsOptions("pants_workdir = /Users/abc/workspace/intellij-pants-plugin/.pants.d (from HARDCODED)");
    Optional<String> workdirOption = options.get(PantsConstants.PANTS_OPTION_PANTS_WORKDIR);
    assertTrue(workdirOption.isPresent());
    assertEquals("/Users/abc/workspace/intellij-pants-plugin/.pants.d",
                 workdirOption.get());
  }

  public void testWorkdirWithSpace() {
    PantsOptions options = new PantsOptions("pants_workdir = /Users/abc/workspace/intellij-pants-plugin/.pants.d (from HARDCODED)");
    Optional<String> spaceOption = options.get(PantsConstants.PANTS_OPTION_PANTS_WORKDIR);
    assertTrue(spaceOption.isPresent());
    assertEquals("/Users/abc/workspace/intellij-pants-plugin/.pants.d",
                 spaceOption.get());
  }

  public void testInvalidWorkdir() {
    PantsOptions options = new PantsOptions("/Users/abc/workspace/intellij-pants-plugin/.pants.d (from HARDCODED)");
    assertFalse(options.get(PantsConstants.PANTS_OPTION_PANTS_WORKDIR).isPresent());
  }

  public void testStrictJvmVersion() {
    PantsOptions options = new PantsOptions("test.junit.strict_jvm_version = False (from HARDCODED)");
    Optional<String> strictJvmOption = options.get(PantsConstants.PANTS_OPTION_TEST_JUNIT_STRICT_JVM_VERSION);
    assertTrue(strictJvmOption.isPresent());
    assertEquals("False", strictJvmOption.get());
  }

  public void testOptionsCache() {
    PantsOptions options_a = PantsOptions.getPantsOptions("./pants");
    PantsOptions options_b = PantsOptions.getPantsOptions("./pants");
    // option_b should be cached result, so identical to options_a
    assertTrue( options_a == options_b);
    PantsOptions.clearCache();
    PantsOptions options_c = PantsOptions.getPantsOptions("./pants");
    assertTrue( options_a != options_c);
  }

  public void testOptionsException() {
    try {
      PantsOptions.getPantsOptions("some_invalid_pants_path");
      fail(String.format("%s should have been thrown.", PantsException.class));
    }
    catch (PantsException ignored) {

    }
  }
}
