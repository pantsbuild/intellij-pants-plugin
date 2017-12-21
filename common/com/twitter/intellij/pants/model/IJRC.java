// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * .ij.import.rc file placed directly under the build root, to configure options to add to / remove from the existing Pants calling procedure.
 * <p>
 * For example:
 * {
 * 'importArgs': {
 * '+': [
 * '--resolver-resolver=coursier'
 * ],
 * '-': [
 * '--no-quiet'
 * ]
 * }
 * }
 * means that at project import and refresh stage, `--resolver-resolver=coursier` will be added to the Pants command line, and `--no-quiet`
 * will be removed from the command line if it existed before.
 */
public class IJRC {

  public static final String IMPORT_RC_FILENAME = ".ij.import.rc";

  private Map<String, List<String>> importArgs;

  // TODO(wisechengyi): add functionality for runConfiguration stage.

  public static Optional<String> getImportPantsRc(final Project myProject) {
    Optional<VirtualFile> root = PantsUtil.findBuildRoot(myProject);
    if (!root.isPresent()) {
      return Optional.empty();
    }
    return getImportPantsRc(root.get().getPath());
  }

  public static Optional<String> getImportPantsRc(@NotNull final String buildRoot) {
    // At import time.
    File importRc = new File(buildRoot, IMPORT_RC_FILENAME);
    if (importRc.isFile()) {
      return Optional.of("--pantsrc-files=" + importRc.getPath());
    }
    return Optional.empty();
  }
}