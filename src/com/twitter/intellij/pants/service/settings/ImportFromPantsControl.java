package com.twitter.intellij.pants.service.settings;

import com.intellij.openapi.externalSystem.service.settings.AbstractImportFromExternalSystemControl;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.project.ProjectManager;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.settings.PantsProjectSettingsControl;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.settings.PantsSettingsListener;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImportFromPantsControl
  extends AbstractImportFromExternalSystemControl<PantsProjectSettings, PantsSettingsListener, PantsSettings> {

  public ImportFromPantsControl() {
    super(PantsConstants.SYSTEM_ID, new PantsSettings(ProjectManager.getInstance().getDefaultProject()), getInitialProjectSettings(), true);
  }

  @NotNull
  private static PantsProjectSettings getInitialProjectSettings() {
    return new PantsProjectSettings();
  }

  @Override
  protected void onLinkedProjectPathChange(@NotNull String path) {
  }

  @NotNull
  @Override
  protected ExternalSystemSettingsControl<PantsProjectSettings> createProjectSettingsControl(@NotNull PantsProjectSettings settings) {
    final PantsProjectSettingsControl settingsControl = new PantsProjectSettingsControl(settings);
    settingsControl.hideUseAutoImportBox();
    return settingsControl;
  }

  @Nullable
  @Override
  protected ExternalSystemSettingsControl<PantsSettings> createSystemSettingsControl(@NotNull PantsSettings settings) {
    return null;
  }
}
