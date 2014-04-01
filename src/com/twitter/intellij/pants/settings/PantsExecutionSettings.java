package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by fedorkorotkov
 */
public class PantsExecutionSettings extends ExternalSystemExecutionSettings {
  private List<String> targetNames;

  public PantsExecutionSettings(List<String> targetNames) {
    this.targetNames = targetNames;
  }

  public List<String> getTargetNames() {
    return targetNames;
  }

  public void setTargetNames(@Nullable List<String> targetNames) {
    this.targetNames = targetNames;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PantsExecutionSettings)) return false;
    if (!super.equals(o)) return false;

    PantsExecutionSettings that = (PantsExecutionSettings) o;

    return ContainerUtil.equalsIdentity(targetNames, that.targetNames);
  }

  @Override
  public int hashCode() {
    int result = 0;
    for (String targetName : targetNames) {
      result = 31 * result + targetName.hashCode();
    }
    return result;
  }
}
