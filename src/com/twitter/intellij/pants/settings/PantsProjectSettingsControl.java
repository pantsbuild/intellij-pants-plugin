package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

public class PantsProjectSettingsControl extends AbstractExternalProjectSettingsControl<PantsProjectSettings> {

  public PantsProjectSettingsControl(@NotNull PantsProjectSettings settings) {
    super(settings);
  }

  @Override
  protected void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel) {
  }

  @Override
  protected boolean isExtraSettingModified() {
    return false;
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {
  }

  @Override
  protected void applyExtraSettings(@NotNull PantsProjectSettings settings) {
  }

  @Override
  public boolean validate(@NotNull PantsProjectSettings settings) throws ConfigurationException {
    return true;
  }
}
