package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import org.jetbrains.annotations.Nullable;

/**
 * Created by fedorkorotkov
 */
public class PantsExecutionSettings extends ExternalSystemExecutionSettings {
  @Nullable
  private String targetName;

  public PantsExecutionSettings(@Nullable String targetName) {
    this.targetName = targetName;
  }

  public String getTargetName() {
    return targetName;
  }

  public void setTargetName(String targetName) {
    this.targetName = targetName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PantsExecutionSettings)) return false;
    if (!super.equals(o)) return false;

    PantsExecutionSettings that = (PantsExecutionSettings) o;

    return !(targetName != null ? !targetName.equals(that.targetName) : that.targetName != null);

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (targetName != null ? targetName.hashCode() : 0);
    return result;
  }
}
