// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * PantsCompileAllTargetsInModuleAction is a UI action that is used to compile all Pants targets for a module
 */
public class PantsCompileAllTargetsInModuleAction extends PantsCompileActionBase {

  public final Optional<Module> module;

  public PantsCompileAllTargetsInModuleAction(Optional<Module> module) {
    super("Compile all targets in module");
    this.module = module;
  }

  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    Optional<Module> module = this.module;
    if (!module.isPresent()) {
      module = PantsUtil.getFileForEvent(e)
        .flatMap(file -> PantsUtil.getModuleForFile(file, project));
    }
    return module
      .map(PantsUtil::getNonGenTargetAddresses)
      .orElse(new LinkedList<>())
      .stream();
  }
}
