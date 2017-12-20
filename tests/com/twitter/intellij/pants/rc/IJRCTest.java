// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.rc;

import com.google.common.collect.Lists;
import com.intellij.execution.configurations.GeneralCommandLine;
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

  public void testProcessCommand() throws IOException {
    FileUtil.writeToFile(temp, "{\n" +
                               "  'importArgs': {\n" +
                               "    '+': [\n" +
                               "      '--resolver-resolver=coursier'\n" +
                               "    ],\n" +
                               "    '-': [\n" +
                               "      '--no-quiet'\n" +
                               "    ]\n" +
                               "  }\n" +
                               "}\n");
    Optional<IJRC> rc = IJRC.getPantsRc(temp.getParent());
    assertTrue(rc.isPresent());

    GeneralCommandLine exportCmd = new GeneralCommandLine().withExePath("abc").withParameters("export", "--no-quiet");
    GeneralCommandLine rcProcessedExportCmd = rc.get().processCommand(exportCmd, IJRC.STAGE_IMPORT);

    assertEquals(exportCmd.getWorkDirectory(), rcProcessedExportCmd.getWorkDirectory());
    assertEquals(exportCmd.getExePath(), rcProcessedExportCmd.getExePath());
    // --resolver-resolver=coursier should be added, and '--no-quiet' should be removed.
    assertEquals(Lists.newArrayList("--resolver-resolver=coursier", "export"), rcProcessedExportCmd.getParametersList().getParameters());
  }
}
