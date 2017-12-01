// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.rc;

import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * RC file to configure options to add to / remove from the existing Pants calling procedure.
 * <p>
 * For example:
 * {
 * 'importArgs': {
 * '+': ['--resolver-resolver=coursier']
 * '-': ['--no-quiet']
 * },
 * runConfiguration: {
 * '+': ['--resolver-resolver=coursier']
 * '-': ['--no-quiet']
 * }
 * }
 */

public class IJRC {

  public static final String rcFilename = ".ijrc";

  private Map<String, List<String>> importArgs;
  // TODO add functionality for runConfiguration

  public static Optional<IJRC> getPantsRc(final Project myProject) {
    Optional<VirtualFile> root = PantsUtil.findBuildRoot(myProject);
    if (!root.isPresent()) {
      return Optional.empty();
    }
    return getPantsRc(root.get().getPath());
  }

  public static Optional<IJRC> getPantsRc(@NotNull final String buildRoot) {
    File rcFilePath = new File(buildRoot, rcFilename);
    if (!rcFilePath.exists()) {
      return Optional.empty();
    }
    Gson gson = new Gson();
    try {
      String json = FileUtil.loadFile(rcFilePath);
      IJRC rc = gson.fromJson(json, IJRC.class);
      return Optional.of(rc);
    }
    catch (IOException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  public List<String> getImportArgsAdditions() {
    return importArgs.get("+");
  }

  public void setImportArgs(Map<String, List<String>> importArgs) {
    this.importArgs = importArgs;
  }
}