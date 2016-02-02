// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model.impl;

import com.twitter.intellij.pants.jps.incremental.model.JpsPantsModuleExtension;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementBase;

import java.util.Set;

public class JpsPantsModuleExtensionImpl extends JpsElementBase<JpsPantsModuleExtensionImpl> implements JpsPantsModuleExtension {
  private String myConfigPath;
  private Set<String> myTargetAddresses;
  private Set<TargetAddressInfo> myTargetAddressInfoSet;
  public JpsPantsModuleExtensionImpl(@NotNull String configPath, @NotNull Set<String> address, @NotNull Set<TargetAddressInfo> targetAddressInfoSet) {
    myConfigPath = configPath;
    myTargetAddresses = address;
    myTargetAddressInfoSet = targetAddressInfoSet;
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
    return new JpsPantsModuleExtensionImpl(myConfigPath, myTargetAddresses, myTargetAddressInfoSet);
  }

  @NotNull
  @Override
  public Set<TargetAddressInfo> getTargetAddressInfoSet() { return myTargetAddressInfoSet;}

  @Override
  public void setTargetAddressInfoSet(@NotNull Set<TargetAddressInfo> targetAddressInfoSet){
    myTargetAddressInfoSet = targetAddressInfoSet;
  }

  @Override
  public void applyChanges(@NotNull JpsPantsModuleExtensionImpl modified) {
    modified.setConfigPath(getConfigPath());
    modified.setTargetAddresses(getTargetAddresses());
    modified.setTargetAddressInfoSet(getTargetAddressInfoSet());
  }
}
