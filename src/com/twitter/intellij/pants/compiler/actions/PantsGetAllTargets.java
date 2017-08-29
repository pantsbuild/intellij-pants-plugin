// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

public class PantsGetAllTargets implements IPantsGetTargets {
  /**
   * Find all targets in the project.
   */
  @NotNull
  @Override
  public Stream<String> apply(Optional<VirtualFile> vf, @NotNull Project project) {
    return Arrays.stream(ModuleManager.getInstance(project).getModules())
      .map(PantsUtil::getNonGenTargetAddresses)
      .flatMap(Collection::stream);
  }
}
