package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by fedorkorotkov
 */
public class PantsProjectSettings extends ExternalProjectSettings {
  @NotNull
  @Override
  public PantsProjectSettings clone() {
    return new PantsProjectSettings();
  }
}
