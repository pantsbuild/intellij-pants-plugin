// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.rc;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.twitter.intellij.pants.model.IJRC;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class IJRCTest extends UsefulTestCase {
  private File temp;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    temp = new File(getHomePath(), IJRC.IMPORT_RC_FILENAME);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    temp.delete();
  }

  public void testInvalidPath() {
    assertFalse(IJRC.getImportPantsRc("/invalid/").isPresent());
  }

  public void testRcPickup() throws IOException {
    FileUtil.writeToFile(temp, "123");
    Optional<String> rc = IJRC.getImportPantsRc(temp.getParent());
    assertTrue(rc.isPresent());
    assertEquals(String.format("--pantsrc-files=%s", temp.getPath()), rc.get());
  }
}
