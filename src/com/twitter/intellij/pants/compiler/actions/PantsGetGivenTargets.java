// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

public class PantsGetGivenTargets implements IPantsGetTargets {

  private final @NotNull Stream<String> targets;

  public PantsGetGivenTargets(String... targets) {
    this.targets = Arrays.stream(targets);
  }

  @NotNull
  @Override
  public Stream<String> apply(Optional<VirtualFile> vf, @NotNull Project project) {
    return this.targets;
  }
}
