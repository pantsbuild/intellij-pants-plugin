// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * PantsCompileAllTargetsAction is a UI action that, when in a project, compiles all targets in the project
 */
public class PantsCompileAllTargetsAction extends PantsCompileActionBase {

  public PantsCompileAllTargetsAction() {
    super("Compile all targets in project");
  }

  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    Module[] modules = ModuleManager.getInstance(project).getModules();
    return Arrays.stream(modules)
      .map(PantsUtil::getNonGenTargetAddresses)
      .flatMap(Collection::stream);
  }
}
