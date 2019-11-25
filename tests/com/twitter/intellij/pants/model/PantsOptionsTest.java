// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsConstants;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.Arrays;
import java.util.Optional;


public class PantsOptionsTest extends TestCase {

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

  public static final String PANTS_OPTION_TEST_JUNIT_OPTIONS = "jvm.test.junit.options";

  public void testEmptyListSetting() {
    PantsOptions options = new PantsOptions("jvm.test.junit.options = [] (from HARDCODED)");
    Optional<String[]> jvmOptions = options.getList(PantsConstants.PANTS_OPTION_TEST_JUNIT_OPTIONS);
    assertTrue(jvmOptions.isPresent());
    assertEquals(0, jvmOptions.get().length);
  }

  public void testNonEmptyListSetting() {
    PantsOptions options = new PantsOptions("jvm.test.junit.options = [\"-Doption1=1\",\"-Doption2=2\"] (from HARDCODED)");
    Optional<String[]> jvmOptions = options.getList(PantsConstants.PANTS_OPTION_TEST_JUNIT_OPTIONS);
    assertTrue(jvmOptions.isPresent());
    Assert.assertArrayEquals(new String[]{"-Doption1=1", "-Doption2=2"}, jvmOptions.get());
  }
}
