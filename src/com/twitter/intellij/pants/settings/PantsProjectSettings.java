package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by fedorkorotkov
 */
public class PantsProjectSettings extends ExternalProjectSettings {
  List<String> myTargets = ContainerUtilRt.newArrayList();

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
