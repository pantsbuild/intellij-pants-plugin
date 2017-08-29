// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.icons.AllIcons;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsAction;
import com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsInModuleAction;
import com.twitter.intellij.pants.compiler.actions.PantsRebuildAction;
import com.twitter.intellij.pants.components.PantsInitComponent;
import com.twitter.intellij.pants.metrics.PantsMetrics;
import com.twitter.intellij.pants.ui.PantsOverrideAction;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
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
      ((IdeaPluginDescriptorImpl) plugin).setPath(new File(basePath));
    }

    registerPantsActions();
  }

  @Override
  public void disposeComponent() {
    PantsUtil.scheduledThreadPool.shutdown();
    PantsMetrics.indexThreadPool.shutdown();
  }

  //  Registers the rebuild action to Pants rebuild action.
  //  Disables compile action
  private void registerPantsActions() {
    ActionManager actionManager = ActionManager.getInstance();

    AnAction pantsCompileAllTargetAction = new PantsOverrideAction(
      PantsConstants.ACTION_MAKE_PROJECT_ID,
      PantsConstants.ACTION_MAKE_PROJECT_DESCRIPTION,
      new PantsCompileAllTargetsAction(),
      AllIcons.Actions.Compile
    );
    AnAction pantsMakeModuleAction = new PantsOverrideAction(
      IdeActions.ACTION_MAKE_MODULE,
      new PantsCompileAllTargetsInModuleAction()
    );
    //  Disables compile option (not applicable in Pants).
    AnAction pantsDisableCompileAction = PantsOverrideAction.createDisabledEmptyAction(IdeActions.ACTION_COMPILE);

    AnAction pantsRebuildAction = new PantsOverrideAction(
      IdeActions.ACTION_COMPILE_PROJECT,
      PantsConstants.REBUILD_PROJECT_DESCRIPTION,
      new PantsRebuildAction()
    );


    actionManager.unregisterAction(PantsConstants.ACTION_MAKE_PROJECT_ID);
    actionManager.unregisterAction(IdeActions.ACTION_MAKE_MODULE);
    actionManager.unregisterAction(IdeActions.ACTION_COMPILE);
    actionManager.unregisterAction(IdeActions.ACTION_COMPILE_PROJECT);

    actionManager.registerAction(PantsConstants.ACTION_MAKE_PROJECT_ID, pantsCompileAllTargetAction);
    actionManager.registerAction(IdeActions.ACTION_MAKE_MODULE, pantsMakeModuleAction);
    actionManager.registerAction(IdeActions.ACTION_COMPILE, pantsDisableCompileAction);
    actionManager.registerAction(IdeActions.ACTION_COMPILE_PROJECT, pantsRebuildAction);
  }
}
