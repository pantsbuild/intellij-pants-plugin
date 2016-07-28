// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

/**
 * PantsCompileAllTargetsInModuleAction is a UI action that is used to compile all Pants targets for a module
 */
public class PantsCompileAllTargetsInModuleAction extends PantsCompileActionBase {

  private Module myModule;

  public PantsCompileAllTargetsInModuleAction(Module module) {
    super("Compile all targets in module");
    myModule = module;
  }

  public PantsCompileAllTargetsInModuleAction() {
    super("Compile all targets in module");
  }

  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    if (myModule == null) {
      VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
      if (file == null) {
        return Stream.empty();
      }
      myModule = ModuleUtil.findModuleForFile(file, project);
    }

    return PantsUtil.getNonGenTargetAddresses(myModule).stream();
  }
}
