// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.openapi.vfs.LocalFileSystem;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.TempFile;

import java.io.IOException;
import java.util.Optional;

import static com.twitter.intellij.pants.execution.PantsMakeBeforeRun.ERROR_TAG;

// Need Integration test framework because LocalFileSystem needs to be initialized.
public class PantsMakeMessageTest extends OSSPantsIntegrationTest {


  public void testErrorMessageWithFilePath() {

    try (TempFile tempFile = TempFile.create("pants_export_run", ".out")) {

      Optional<PantsMakeBeforeRun.ParseResult> result = PantsMakeBeforeRun.parseErrorLocation(
        "                       [error] " + tempFile.getFile().getAbsolutePath() + ":23:1: cannot find symbol",
        ERROR_TAG
      );
      assertTrue(result.isPresent());
      assertEquals(
        LocalFileSystem.getInstance()
          .findFileByIoFile(tempFile.getFile()),
        result.get().getFile()
      );
      assertEquals(23, result.get().getLineNumber());
      assertEquals(1, result.get().getColumnNumber());
    }
    catch (IOException e) {
      // Fall-through to handle outside the block.
    }
  }

  public void testErrorMessageWithStrangerFilePath() {

    try (TempFile filePathWithSpace = TempFile.create("pants_exp  ort_run", ".out")) {

      Optional<PantsMakeBeforeRun.ParseResult> result = PantsMakeBeforeRun.parseErrorLocation(
        "                       [error] " + filePathWithSpace.getFile().getAbsolutePath() + ":23:1: cannot find symbol",
        ERROR_TAG
      );
      assertTrue(result.isPresent());
      assertEquals(
        LocalFileSystem.getInstance()
          .findFileByIoFile(filePathWithSpace.getFile()),
        result.get().getFile()
      );
      assertEquals(23, result.get().getLineNumber());
      assertEquals(1, result.get().getColumnNumber());
    }
    catch (IOException e) {
      // Fall-through to handle outside the block.
    }
  }


  public void testErrorMessageWithInvalidFilePath() {
    Optional<PantsMakeBeforeRun.ParseResult> result = PantsMakeBeforeRun.parseErrorLocation(
      "                       [error] /non/existent/file/path:23:1: cannot find symbol",
      ERROR_TAG
    );
    assertFalse(result.isPresent());
  }

  public void testErrorMessageWithNoLocation() {
    Optional<PantsMakeBeforeRun.ParseResult> result = PantsMakeBeforeRun.parseErrorLocation(
      "                       [error]     String greeting = Greeting.greetFromRXesource(\"org/pantsbuild/example/hello/world.txt\");\n",
      ERROR_TAG
    );
    assertFalse(result.isPresent());
  }
}
