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

    // todo: deal with external process
    //final boolean inProcess = Boolean.valueOf(System.getProperty(key.toLowerCase()));
    Registry.get(key).setValue(true);
  }

  @Override
  public void disposeComponent() {

  }
}
