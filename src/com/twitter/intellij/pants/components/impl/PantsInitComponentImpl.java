// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.util.registry.Registry;
import com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsAction;
import com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsInModuleAction;
import com.twitter.intellij.pants.compiler.actions.PantsRebuildAction;
import com.twitter.intellij.pants.components.PantsInitComponent;
import com.twitter.intellij.pants.metrics.PantsMetrics;
import com.twitter.intellij.pants.ui.PantsOverrideAction;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class PantsInitComponentImpl implements PantsInitComponent {

  public static final String PANTS_REBUILD_ACTION_NAME = IdeActions.ACTION_COMPILE_PROJECT + "Pants";
  public static final String PANTS_COMPILE_MODULE_ACTION_NAME = IdeActions.ACTION_MAKE_MODULE + "Pants";
  public static final String PANTS_COMPILE_PROJECT_ACTION_NAME = PantsConstants.ACTION_MAKE_PROJECT_ID + "Pants";

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

    registerPantsActions();
  }

  @Override
  public void disposeComponent() {
    PantsUtil.scheduledThreadPool.shutdown();
    PantsMetrics.globalCleanup();
  }

  //  Registers the rebuild action to Pants rebuild action.
  //  Registers Make module action to 'Make all targets in module' action.
  //  Disables compile action
  private void registerPantsActions() {
    ActionManager actionManager = ActionManager.getInstance();

    AnAction pantsCompileAllTargetAction = new PantsOverrideAction(
      PantsConstants.ACTION_MAKE_PROJECT_ID,
      PantsConstants.ACTION_MAKE_PROJECT_DESCRIPTION,
      new PantsCompileAllTargetsAction(),
      PantsIcons.Icon
    );

    AnAction pantsMakeModuleAction = new PantsOverrideAction(
      PANTS_COMPILE_MODULE_ACTION_NAME,
      new PantsCompileAllTargetsInModuleAction(Optional.empty())
    );

    AnAction pantsRebuildAction = new PantsOverrideAction(
      PANTS_REBUILD_ACTION_NAME,
      PantsConstants.REBUILD_PROJECT_DESCRIPTION,
      new PantsRebuildAction()
    );

    actionManager.registerAction(PANTS_COMPILE_PROJECT_ACTION_NAME, pantsCompileAllTargetAction);
    actionManager.registerAction(PANTS_COMPILE_MODULE_ACTION_NAME, pantsMakeModuleAction);
    actionManager.registerAction(PANTS_REBUILD_ACTION_NAME, pantsRebuildAction);
  }
}
