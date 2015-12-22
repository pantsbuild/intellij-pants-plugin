// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.components.PantsInitComponent;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class PantsInitComponentImpl implements PantsInitComponent {
  @NotNull
  @Override
  public String getComponentName() {
    return "pants.init";
  }

  @Override
  public void initComponent() {
    // The Pants plugin doesn't do so many computations for building a project
    // to start an external JVM each time.
    // The plugin only calls `export` goal and parses JSON response.
    // So it will be in process all the time.
    final String key = PantsConstants.SYSTEM_ID.getId() + ExternalSystemConstants.USE_IN_PROCESS_COMMUNICATION_REGISTRY_KEY_SUFFIX;
    Registry.get(key).setValue(true);

    // hack to trick BuildProcessClasspathManager
    final String basePath = System.getProperty("pants.plugin.base.path");
    final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(PantsConstants.PLUGIN_ID));
    if (StringUtil.isNotEmpty(basePath) && plugin instanceof IdeaPluginDescriptorImpl) {
      ((IdeaPluginDescriptorImpl)plugin).setPath(new File(basePath));
    }
    
    // Add (Cmd Shift R) as shortcut to refresh the project if there is no shortcut for that action yet.
    if (KeymapManager.getInstance().getActiveKeymap().getShortcuts("ExternalSystem.RefreshAllProjects").length == 0) {
      KeymapManager.getInstance().getActiveKeymap()
        .addShortcut("ExternalSystem.RefreshAllProjects", KeyboardShortcut.fromString("shift meta pressed R"));
    }
  }

  @Override
  public void disposeComponent() {

  }
}
