// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Wrapper temporary file that is auto closable.
 */
public class Tempfile implements AutoCloseable {
  private final File file;

  public Tempfile(String prefix, String suffix) throws IOException {
    file = File.createTempFile(prefix, suffix);
  }

  public File getFile() {
    return file;
  }

  @Override
  public void close() throws IOException {
    Files.deleteIfExists(file.toPath());
  }
}
