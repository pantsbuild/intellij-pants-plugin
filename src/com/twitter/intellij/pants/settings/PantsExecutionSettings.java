// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Created by fedorkorotkov
 */
public class PantsExecutionSettings extends ExternalSystemExecutionSettings {
  private final boolean myAllTargets;
  private List<String> myTargetNames;
  @NotNull private final List<String> myResolverExtensionClassNames = ContainerUtilRt.newArrayList();

  public PantsExecutionSettings() {
    this(Collections.<String>emptyList(), true);
  }

  public PantsExecutionSettings(List<String> targetNames, boolean allTargets) {
    myTargetNames = targetNames;
    myAllTargets = allTargets;
  }

  public List<String> getTargetNames() {
    return myTargetNames;
  }

  public boolean isAllTargets() {
    return myAllTargets;
  }

  @NotNull
  public List<String> getResolverExtensionClassNames() {
    return myResolverExtensionClassNames;
  }

  public void addResolverExtensionClassNams(@NotNull String className) {
    myResolverExtensionClassNames.add(className);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PantsExecutionSettings)) return false;
    if (!super.equals(o)) return false;

    PantsExecutionSettings that = (PantsExecutionSettings)o;

    return ContainerUtil.equalsIdentity(myTargetNames, that.myTargetNames);
  }

  @Override
  public int hashCode() {
    int result = 0;
    for (String targetName : myTargetNames) {
      result = 31 * result + targetName.hashCode();
    }
    return result;
  }
}
