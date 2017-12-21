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

  // TODO(wisechengyi): add functionality for runConfiguration stage.

  public static Optional<String> getImportPantsRc(@NotNull final String buildRoot) {
    // At import time.
    File importRc = new File(buildRoot, IMPORT_RC_FILENAME);
    if (importRc.isFile()) {
      return Optional.of("--pantsrc-files=" + importRc.getPath());
    }
    return Optional.empty();
  }
}