// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImportFromPantsControl
  extends AbstractImportFromExternalSystemControl<PantsProjectSettings, PantsSettingsListener, PantsSettings> {

  public ImportFromPantsControl() {
    super(PantsConstants.SYSTEM_ID, PantsSettings.defaultSettings(), getInitialProjectSettings(), true);
  }

  @NotNull
  private static PantsProjectSettings getInitialProjectSettings() {
    return new PantsProjectSettings();
  }

  @Override
  protected void onLinkedProjectPathChange(@NotNull String path) {
    if (!StringUtil.isEmpty(path)) {
      ((PantsProjectSettingsControl)getProjectSettingsControl()).onProjectPathChanged(path);
    }
  }

  @NotNull
  @Override
  protected ExternalSystemSettingsControl<PantsProjectSettings> createProjectSettingsControl(@NotNull PantsProjectSettings settings) {
    return new PantsProjectSettingsControl(settings);
  }

  @Nullable
  @Override
  protected ExternalSystemSettingsControl<PantsSettings> createSystemSettingsControl(@NotNull PantsSettings settings) {
    return null;
  }
}
