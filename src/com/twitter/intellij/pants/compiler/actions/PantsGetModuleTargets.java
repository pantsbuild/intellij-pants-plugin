// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Stream;

public class PantsGetModuleTargets implements IPantsGetTargets {

  private final Optional<Module> module;

  public PantsGetModuleTargets() {
    this(Optional.empty());
  }

  public PantsGetModuleTargets(@Nullable Module module) {
    this(Optional.ofNullable(module));
  }

  public PantsGetModuleTargets(Optional<Module> module) {
    this.module = module;
  }

  @NotNull
  @Override
  public Stream<String> apply(Optional<VirtualFile> vf, @NotNull Project project) {
    Optional<Module> module = this.module;
    if (!module.isPresent()) {
      module = vf.flatMap(file -> Optional.ofNullable(ModuleUtil.findModuleForFile(file, project)));
    }
    return module
      .map(PantsUtil::getNonGenTargetAddresses)
      .orElse(new LinkedList<>())
      .stream();
  }
}
