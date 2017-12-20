// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.intellij.execution.configurations.GeneralCommandLine;
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
 * .ijrc file, typically directly under the build root, to configure options to add to / remove from the existing Pants calling procedure.
 *
 * For example:
 * {
 *   'importArgs': {
 *     '+': [
 *       '--resolver-resolver=coursier'
 *     ],
 *     '-': [
 *       '--no-quiet'
 *     ]
 *   }
 * }
 * means that at project import and refresh stage, `--resolver-resolver=coursier` will be added to the Pants command line, and `--no-quiet`
 * will be removed from the command line if it existed before.
 */
public class IJRC {

  public static final String rcFilename = ".ijrc";

  public static final String STAGE_IMPORT = "importArgs";

  private Map<String, List<String>> importArgs;
  // TODO(wisechengyi): add functionality for runConfiguration

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

  /**
   * @param stage the stage where the process is happening in a Pants project.
   * @return
   */
  private List<String> getStageAdditions(String stage) {
    switch (stage) {
      case STAGE_IMPORT:
        return getImportArgsAdditions();
      default:
        return Lists.newArrayList();
    }
  }

  /**
   * @param stage the stage where the process is happening in a Pants project.
   * @return
   */
  private List<String> getStageRemovals(String stage) {
    switch (stage) {
      case STAGE_IMPORT:
        return getImportArgsRemovals();
      default:
        return Lists.newArrayList();
    }
  }

  public List<String> getImportArgsAdditions() {
    return Optional.ofNullable(importArgs.get("+")).orElse(Lists.newArrayList());
  }

  public List<String> getImportArgsRemovals() {
    return Optional.ofNullable(importArgs.get("-")).orElse(Lists.newArrayList());
  }

  public void setImportArgs(Map<String, List<String>> importArgs) {
    this.importArgs = importArgs;
  }

  public GeneralCommandLine processCommand(GeneralCommandLine cmd, String stage) {

    // 1. Initiate with additions (This assumes that all additional options are fully scoped)
    // 2. Add the args from original command
    // 3. Filter out the ones to remove by exact string match

    List<String> newArgs = Lists.newArrayList(getStageAdditions(stage));
    newArgs.addAll(cmd.getParametersList().getParameters());
    newArgs.removeAll(getStageRemovals(stage));

    return new GeneralCommandLine().withParameters(newArgs)
      .withWorkDirectory(cmd.getWorkDirectory())
      .withExePath(cmd.getExePath())
      .withEnvironment(cmd.getEnvironment())
      .withCharset(cmd.getCharset());
  }
}