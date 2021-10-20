// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Optional;


/**
 * This class tries to find the pantsrc files associated with the pants repo and construct the command line arg if they exist.
 */
public class IJRC {

  public static final String IMPORT_RC_FILENAME = ".ij.import.rc";
  public static final String ITERATE_RC_FILENAME = ".ij.iterate.rc";

  // TODO(wisechengyi): add functionality for runConfiguration stage.

  // At import time.
  public static Optional<String> getImportPantsRc(@NotNull final String buildRoot) {
    return getPantsRc(buildRoot, IMPORT_RC_FILENAME);
  }

  public static Optional<String> getIteratePantsRc(@NotNull final String buildRoot) {
    return getPantsRc(buildRoot, ITERATE_RC_FILENAME);
  }

  private static Optional<String> getPantsRc(@NotNull final String buildRoot, String rcFilename) {
    // At import time.
    File rcFile = new File(buildRoot, rcFilename);
    if (rcFile.isFile()) {
      return Optional.of("--pantsrc-files=" + rcFile.getPath());
    }
    return Optional.empty();
  }
}