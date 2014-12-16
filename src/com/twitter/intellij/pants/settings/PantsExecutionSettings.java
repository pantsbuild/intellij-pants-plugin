// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.util.containers.ContainerUtilRt;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PantsExecutionSettings extends ExternalSystemExecutionSettings {
  private final boolean myAllTargets;
  private final boolean myCompileWithIntellij;
  private List<String> myTargetNames;
  @NotNull private final List<String> myResolverExtensionClassNames = ContainerUtilRt.newArrayList();

  public PantsExecutionSettings() {
    this(Collections.<String>emptyList(), true, false);
  }

  public PantsExecutionSettings(List<String> targetNames, boolean allTargets, boolean compileWithIntellij) {
    myTargetNames = targetNames;
    myAllTargets = allTargets;
    myCompileWithIntellij = compileWithIntellij;
  }

  public List<String> getTargetNames() {
    return myTargetNames;
  }

  public boolean isAllTargets() {
    return myAllTargets;
  }

  public boolean isCompileWithIntellij() {
    return myCompileWithIntellij;
  }

  @NotNull
  public List<String> getResolverExtensionClassNames() {
    return myResolverExtensionClassNames;
  }

  public void addResolverExtensionClassName(@NotNull String className) {
    myResolverExtensionClassNames.add(className);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    PantsExecutionSettings settings = (PantsExecutionSettings)o;

    if (myAllTargets != settings.myAllTargets) return false;
    if (myCompileWithIntellij != settings.myCompileWithIntellij) return false;
    if (!myResolverExtensionClassNames.equals(settings.myResolverExtensionClassNames)) return false;
    if (myTargetNames != null ? !myTargetNames.equals(settings.myTargetNames) : settings.myTargetNames != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (myAllTargets ? 1 : 0);
    result = 31 * result + (myTargetNames != null ? myTargetNames.hashCode() : 0);
    result = 31 * result + myResolverExtensionClassNames.hashCode();
    result = 31 * result + (myCompileWithIntellij ? 1 : 0);
    return result;
  }
}
