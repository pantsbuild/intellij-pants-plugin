// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * PantsCompileTargetAction is a UI action that is used to compile a Pants target or collection of targets
 */
public class PantsCompileTargetAction extends PantsCompileActionBase {

  private final @NotNull String targetAddress;

  public PantsCompileTargetAction(@NotNull String targetAddress) {
    super(String.format("Compile target '%s'", targetAddress));
    this.targetAddress = targetAddress;
  }

  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    return Stream.of(targetAddress);
  }
}
