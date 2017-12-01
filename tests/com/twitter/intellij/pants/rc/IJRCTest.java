// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.rc;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.UsefulTestCase;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class IJRCTest extends UsefulTestCase {
  private File temp;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    temp = new File(getHomePath(), IJRC.rcFilename);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    temp.delete();
  }

  public void testRc() throws IOException {
    FileUtil.writeToFile(temp, "{\n" +
                               "'importArgs': {\n" +
                               " '+': ['--resolver-resolver=coursier']\n" +
                               "}\n" +
                               "}");
    Optional<IJRC> rc = IJRC.getPantsRc(temp.getParent());
    assertTrue(rc.isPresent());
    assertEquals(Lists.newArrayList("--resolver-resolver=coursier"), rc.get().getImportArgsAdditions());
    assertTrue(rc.get().getImportArgsRemovals().isEmpty());
  }

  public void testInvalidPath() {
    assertFalse(IJRC.getPantsRc("/invalid/").isPresent());
  }

  public void testInvalidJson() throws IOException {
    FileUtil.writeToFile(temp, "123");

    try {
      Optional<IJRC> rc = IJRC.getPantsRc(temp.getParent());
      fail("Should throw com.google.gson.JsonSyntaxException, but passed.");
    }
    catch (com.google.gson.JsonSyntaxException ignored) {
    }
  }
}
