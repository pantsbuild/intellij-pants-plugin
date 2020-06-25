// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class AmendData {
  @NotNull
  private final PantsBspData myPantsBspData;

  @NotNull
  private final Collection<String> myTargetSpecs;

  public AmendData(@NotNull PantsBspData pantsBspData, @NotNull Collection<String> targetSpecs){
    myPantsBspData = pantsBspData;
    myTargetSpecs = targetSpecs;
  }

  @NotNull
  public PantsBspData getPantsBspData() {
    return myPantsBspData;
  }

  @NotNull
  public Collection<String> getTargetSpecs() {
    return myTargetSpecs;
  }
}
