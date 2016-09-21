// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.BuildTargetLoader;
import org.jetbrains.jps.builders.BuildTargetType;
import org.jetbrains.jps.builders.impl.BuildTargetRegistryImpl;
import org.jetbrains.jps.model.JpsModel;

import java.util.Collections;
import java.util.List;

public class PantsBuildTargetType extends BuildTargetType<PantsBuildTarget> {
  public static PantsBuildTargetType INSTANCE = new PantsBuildTargetType();

  /**
   * Life cycle of `myPantsBuildTarget`:
   * 1. Created under {@link BuildTargetRegistryImpl#BuildTargetRegistryImpl, type.computeAllTargets(model)}.
   * 2. {@link org.jetbrains.jps.cmdline.BuildRunner#createCompilationScope} calls {@link BuildTargetLoader#createTarget}
   *    for every target address previously added in PantsBuildTargetScopeProvider.
   * 3. Finally myPantsBuildTarget gets passed as `target` in {@link com.twitter.intellij.pants.jps.incremental.PantsTargetBuilder#build}
   */
  private PantsBuildTarget myPantsBuildTarget;

  protected PantsBuildTargetType() {
    super("pants-goal-compile");
  }

  @NotNull
  @Override
  public List<PantsBuildTarget> computeAllTargets(@NotNull JpsModel model) {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public BuildTargetLoader<PantsBuildTarget> createLoader(@NotNull final JpsModel model) {
    return new BuildTargetLoader<PantsBuildTarget>() {
      @Nullable
      @Override
      public PantsBuildTarget createTarget(@Nullable String targetAddress) {
        return myPantsBuildTarget;
      }
    };
  }
}
