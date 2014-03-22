package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by fedorkorotkov
 */
public class PantsProjectSettings extends ExternalProjectSettings {
  @Nullable
  private String pantsExecutablePath = null;

  public PantsProjectSettings() {
  }

  public PantsProjectSettings(@Nullable String pantsExecutablePath) {
    this.pantsExecutablePath = pantsExecutablePath;
  }

  @Nullable
  public String getPantsExecutablePath() {
    return pantsExecutablePath;
  }

  public void setPantsExecutablePath(@Nullable String pantsExecutablePath) {
    this.pantsExecutablePath = pantsExecutablePath;
  }

  @NotNull
  @Override
  public PantsProjectSettings clone() {
    return new PantsProjectSettings(pantsExecutablePath);
  }
}
