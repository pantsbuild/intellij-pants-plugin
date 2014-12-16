// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model;

import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

import java.util.List;

public interface JpsPantsProjectExtension extends JpsElement, PantsCompileOptions {
  public static final JpsElementChildRole<JpsPantsProjectExtension> ROLE = JpsElementChildRoleBase.create(PantsConstants.PANTS);

  void setTargetPath(@NotNull String path);

  void setTargetNames(@NotNull List<String> names);

  boolean isCompileWithIntellij();
}
