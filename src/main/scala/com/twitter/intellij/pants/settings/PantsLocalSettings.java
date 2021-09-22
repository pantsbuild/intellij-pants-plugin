// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "PantsLocalSettings", storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
public class PantsLocalSettings extends AbstractExternalSystemLocalSettings<AbstractExternalSystemLocalSettings.State>
  implements PersistentStateComponent<AbstractExternalSystemLocalSettings.State> {

  public PantsLocalSettings(@NotNull Project project) {
    super(PantsConstants.SYSTEM_ID, project);
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
