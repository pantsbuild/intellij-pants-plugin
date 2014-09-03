package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PantsProjectSettings extends ExternalProjectSettings {
  private List<String> myTargets = ContainerUtilRt.newArrayList();
  private boolean myAllTargets;

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
      ((PantsProjectSettings)receiver).setAllTargets(isAllTargets());
      ((PantsProjectSettings)receiver).setTargets(getTargets());
    }
  }

  public List<String> getTargets() {
    return myTargets;
  }

  public void setTargets(List<String> targets) {
    myTargets = targets;
  }

  public void setAllTargets(boolean allTargets) {
    myAllTargets = allTargets;
  }

  public boolean isAllTargets() {
    return myAllTargets;
  }
}
