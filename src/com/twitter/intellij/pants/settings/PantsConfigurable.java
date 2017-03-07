// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalSystemConfigurable;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsConfigurable extends AbstractExternalSystemConfigurable<PantsProjectSettings, PantsSettingsListener, PantsSettings> {

  public PantsConfigurable(@NotNull Project project) {
    super(project, PantsConstants.SYSTEM_ID);
  }

  @NotNull
  @Override
  protected ExternalSystemSettingsControl<PantsProjectSettings> createProjectSettingsControl(@NotNull PantsProjectSettings settings) {
    return new PantsProjectSettingsControl(settings);
  }

  @Nullable
  @Override
  protected ExternalSystemSettingsControl<PantsSettings> createSystemSettingsControl(@NotNull PantsSettings settings) {
    return new PantsSystemSettingsControl();
  }

  @NotNull
  @Override
  protected PantsProjectSettings newProjectSettings() {
    return new PantsProjectSettings();
  }

  @NotNull
  @Override
  public String getId() {
    return "reference.settingsdialog.project.pants";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }
}
