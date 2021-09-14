// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import org.jetbrains.annotations.NotNull;

public enum PantsSourceType {
  /**
   * Warning: do not change the order of the type declaration,
   * because PantsSourceType.values() returns them in order and
   * is depended on.
   */
  SOURCE(ExternalSystemSourceType.SOURCE),
  TEST(ExternalSystemSourceType.TEST),
  RESOURCE(ExternalSystemSourceType.RESOURCE),
  TEST_RESOURCE(ExternalSystemSourceType.TEST_RESOURCE),
  SOURCE_GENERATED(ExternalSystemSourceType.SOURCE_GENERATED);

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

  public static boolean isResource(@NotNull PantsSourceType root) {
    return RESOURCE.equals(root) || TEST_RESOURCE.equals(root);
  }
}
