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

  public void testInvalidPath() {
    File temp = new File(getHomePath(), IJRC.IMPORT_RC_FILENAME);
    assertFalse(IJRC.getImportPantsRc("/invalid/").isPresent());
    temp.delete();
  }

  public void testRcPickup() throws IOException {
    File temp = new File(getHomePath(), IJRC.IMPORT_RC_FILENAME);
    FileUtil.writeToFile(temp, "123");
    Optional<String> rc = IJRC.getImportPantsRc(temp.getParent());
    assertTrue(rc.isPresent());
    assertEquals(String.format("--pantsrc-files=%s", temp.getPath()), rc.get());
    temp.delete();
  }

  public void testIterateRcPickup() throws IOException {
    File temp = new File(getHomePath(), IJRC.ITERATE_RC_FILENAME);
    FileUtil.writeToFile(temp, "123");
    Optional<String> rc = IJRC.getIteratePantsRc(temp.getParent());
    assertTrue(rc.isPresent());
    assertEquals(String.format("--pantsrc-files=%s", temp.getPath()), rc.get());
    temp.delete();
  }
}
