package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Created by fedorkorotkov
 */
public class PantsProjectSettings extends ExternalProjectSettings {
  private List<String> myTargets = Collections.emptyList();

  @NotNull
  @Override
  public PantsProjectSettings clone() {
    final PantsProjectSettings pantsProjectSettings = new PantsProjectSettings();
    copyTo(pantsProjectSettings);
    return pantsProjectSettings;
  }

  @Override
  protected void copyTo(@NotNull ExternalProjectSettings receiver) {
    super.copyTo(receiver);
    if (receiver instanceof PantsProjectSettings) {
      ((PantsProjectSettings)receiver).setTargets(getTargets());
    }
  }

  public List<String> getTargets() {
    return myTargets;
  }

  public void setTargets(List<String> targets) {
    myTargets = targets;
  }
}
