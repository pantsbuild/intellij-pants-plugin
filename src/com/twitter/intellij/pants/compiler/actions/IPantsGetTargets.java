// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

public interface IPantsGetTargets {
  static Optional<VirtualFile> getFileForEvent(@Nullable AnActionEvent e) {
    return Optional.ofNullable(e)
      .flatMap(ev -> Optional.ofNullable(ev.getData(CommonDataKeys.VIRTUAL_FILE)));
  }

  @NotNull
  Stream<String> apply(Optional<VirtualFile> vf, @NotNull Project project);
}
