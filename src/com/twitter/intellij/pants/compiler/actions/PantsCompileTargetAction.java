// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.stream.Stream;

/**
 * PantsCompileTargetAction is a UI action that is used to compile a Pants target or collection of targets
 */
public class PantsCompileTargetAction extends PantsCompileActionBase {

  private HashSet<String> myTargetAddresses = new HashSet<>();

  public PantsCompileTargetAction(String targetAddress) {
    super(targetAddress);
    myTargetAddresses.add(targetAddress);
  }

  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    return myTargetAddresses.stream();
  }
}
