// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model;

import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

public interface JpsPantsModuleExtension extends JpsElement {
  public static final JpsElementChildRole<JpsPantsModuleExtension> ROLE = JpsElementChildRoleBase.create(PantsConstants.PANTS);

  @NotNull
  String getTargetAddress();

  void setTargetAddress(@NotNull String address);

  /**
   * @return path to a linked BUILD file or folder that contains it
   */
  @NotNull
  String getConfigPath();

  void setConfigPath(@NotNull String configPath);
}