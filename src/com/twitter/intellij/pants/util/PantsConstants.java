// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import org.jetbrains.annotations.NotNull;

/**
 * Created by fedorkorotkov
 */
public class PantsConstants {
  @NotNull
  public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId(PantsUtil.PANTS);
}
