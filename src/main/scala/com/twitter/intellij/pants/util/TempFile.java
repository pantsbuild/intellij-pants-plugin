// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Wrapper temporary file that is auto closable.
 */
public class TempFile implements AutoCloseable {
  private final File file;

  private TempFile(File file) {
    this.file = file;
  }

  public File getFile() {
    return file;
  }

  @Override
  public void close() throws IOException {
    Files.deleteIfExists(file.toPath());
  }

  public static TempFile create(String prefix, String suffix) throws IOException {
    return new TempFile(File.createTempFile(prefix, suffix));
  }
}
