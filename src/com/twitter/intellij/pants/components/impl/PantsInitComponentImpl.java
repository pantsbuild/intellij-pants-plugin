// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.util.registry.Registry;
import com.twitter.intellij.pants.components.PantsInitComponent;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.generate.tostring.util.StringUtil;

import java.io.File;

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

    // hack to trick BuildProcessClasspathManager
    final String basePath = System.getProperty("pants.plugin.base.path");
    final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(PantsConstants.PLUGIN_ID));
    if (StringUtil.isNotEmpty(basePath) && plugin instanceof IdeaPluginDescriptorImpl) {
      ((IdeaPluginDescriptorImpl)plugin).setPath(new File(basePath));
    }
  }

  @Override
  public void disposeComponent() {

  }
}
