// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.components.*;
import com.intellij.openapi.externalSystem.service.project.PlatformFacade;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "PantsLocalSettings", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
public class PantsLocalSettings extends AbstractExternalSystemLocalSettings
  implements PersistentStateComponent<AbstractExternalSystemLocalSettings.State> {

  public PantsLocalSettings(@NotNull Project project, @NotNull PlatformFacade facade) {
    super(PantsConstants.SYSTEM_ID, project, facade);
  }

  @NotNull
  public static PantsLocalSettings getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, PantsLocalSettings.class);
  }

  @Nullable
  @Override
  public State getState() {
    State state = new State();
    fillState(state);
    return state;
  }
}
