// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model;

import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.JpsElement;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;

public interface JpsPantsProjectExtension extends JpsElement {
  JpsElementChildRole<JpsPantsProjectExtension> ROLE = JpsElementChildRoleBase.create(PantsConstants.PANTS);

  @NotNull
  String getPantsExecutablePath();

  void setPantsExecutablePath(@NotNull String path);

  boolean isCompileWithIntellij();

  void setCompileWithIntellij(boolean compileWithIntellij);

  boolean isUseIdeaProjectJdk();
}
