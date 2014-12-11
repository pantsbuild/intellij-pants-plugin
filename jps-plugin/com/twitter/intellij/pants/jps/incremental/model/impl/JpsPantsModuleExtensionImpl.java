// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model.impl;

import com.twitter.intellij.pants.jps.incremental.model.JpsPantsModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementBase;

public class JpsPantsModuleExtensionImpl extends JpsElementBase<JpsPantsModuleExtensionImpl> implements JpsPantsModuleExtension {
  private String myConfigPath;
  private String myTargetAddress;

  public JpsPantsModuleExtensionImpl(@NotNull String configPath, @NotNull String address) {
    myConfigPath = configPath;
    myTargetAddress = address;
  }

  @NotNull
  @Override
  public String getTargetAddress() {
    return myTargetAddress;
  }

  @Override
  public void setTargetAddress(@NotNull String address) {
    myTargetAddress = address;
  }

  @NotNull
  @Override
  public String getConfigPath() {
    return myConfigPath;
  }

  @Override
  public void setConfigPath(@NotNull String configPath) {
    myConfigPath = configPath;
  }

  @NotNull
  @Override
  public JpsPantsModuleExtensionImpl createCopy() {
    return new JpsPantsModuleExtensionImpl(myConfigPath, myTargetAddress);
  }

  @Override
  public void applyChanges(@NotNull JpsPantsModuleExtensionImpl modified) {
    modified.setConfigPath(getConfigPath());
    modified.setTargetAddress(getTargetAddress());
  }
}
