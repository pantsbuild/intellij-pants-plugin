// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.util.registry.Registry;
import com.twitter.intellij.pants.components.PantsInitComponent;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

public class PantsInitComponentImpl implements PantsInitComponent {
  @NotNull
  @Override
  public String getComponentName() {
    return "pants.init";
  }

  @Override
  public void initComponent() {
    // enable inProcessMode for debugging
    final String key = PantsConstants.SYSTEM_ID.getId() + ExternalSystemConstants.USE_IN_PROCESS_COMMUNICATION_REGISTRY_KEY_SUFFIX;

    final boolean inProcess = Boolean.valueOf(System.getProperty(key.toLowerCase()));
    // until bug at RemoteExternalSystemCommunicationManager.java:162 is not fixed
    // we'll always run in inProcess mode
    Registry.get(key).setValue(true/*inProcess*/);
  }

  @Override
  public void disposeComponent() {

  }
}
