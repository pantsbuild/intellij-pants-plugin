// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Stream;

public class PantsGetJustFileTarget implements IPantsGetTargets {
  /**
   * Get the path of the file opened in the selected editor.
   */
  @NotNull
  @Override
  public Stream<String> apply(Optional<VirtualFile> vf, @NotNull Project project) {
    return IPantsGetTargets.getSelectedFile(project)
      .flatMap(file -> Optional.ofNullable(file.getPath()))
      .map(Stream::of)
      .orElse(Stream.empty());
  }
}
