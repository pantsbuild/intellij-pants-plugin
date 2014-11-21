// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PantsSourceType {
  SOURCE(ExternalSystemSourceType.SOURCE),
  TEST(ExternalSystemSourceType.TEST),
  RESOURCE(ExternalSystemSourceType.RESOURCE),
  TEST_RESOURCE(ExternalSystemSourceType.TEST_RESOURCE);

  private ExternalSystemSourceType myExternalType;

  private PantsSourceType(@NotNull ExternalSystemSourceType externalType) {
    myExternalType = externalType;
  }

  @NotNull
  public ExternalSystemSourceType toExternalSystemSourceType() {
    return myExternalType;
  }

  @Override
  public String toString() {
    return myExternalType.toString();
  }

  public static boolean isResource(@Nullable PantsSourceType root) {
    return RESOURCE.equals(root) || TEST_RESOURCE.equals(root);
  }
}
