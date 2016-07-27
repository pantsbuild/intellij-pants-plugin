// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * PantsCompileAllTargetsAction is a UI action that, when in a project, compiles all targets in the project
 */
public class PantsCompileAllTargetsAction extends PantsCompileActionBase {

  protected PantsCompileAllTargetsAction(String name) {
    super(name);
  }

  public PantsCompileAllTargetsAction() {
    this("Compile all targets in project");
  }

  @Nullable
  @Override
  public Stream<PantsTargetAddress> getTargets(AnActionEvent e, @NotNull Project project) {
    return Arrays.stream(ModuleManager.getInstance(project).getModules())
      .map(PantsUtil::getTargetAddressesFromModule)
      .flatMap(Collection::stream);
  }

  @Override
  public boolean doCleanAll() {
    return false;
  }
}
