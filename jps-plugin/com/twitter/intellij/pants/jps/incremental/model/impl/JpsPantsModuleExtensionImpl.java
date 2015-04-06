// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model.impl;

import com.twitter.intellij.pants.jps.incremental.model.JpsPantsModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementBase;

import java.util.Set;

public class JpsPantsModuleExtensionImpl extends JpsElementBase<JpsPantsModuleExtensionImpl> implements JpsPantsModuleExtension {
  private String myConfigPath;
  private Set<String> myTargetAddresses;

  public JpsPantsModuleExtensionImpl(@NotNull String configPath, @NotNull Set<String> address) {
    myConfigPath = configPath;
    myTargetAddresses = address;
  }

  @NotNull
  @Override
  public Set<String> getTargetAddresses() {
    return myTargetAddresses;
  }

  @Override
  public void setTargetAddresses(@NotNull Set<String> addresses) {
    myTargetAddresses = addresses;
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
    return new JpsPantsModuleExtensionImpl(myConfigPath, myTargetAddresses);
  }

  @Override
  public void applyChanges(@NotNull JpsPantsModuleExtensionImpl modified) {
    modified.setConfigPath(getConfigPath());
    modified.setTargetAddresses(getTargetAddresses());
  }
}
