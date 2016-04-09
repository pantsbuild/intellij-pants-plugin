// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.util.containers.ContainerUtilRt;
import com.twitter.intellij.pants.model.PantsCompileOptions;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PantsProjectSettings extends ExternalProjectSettings implements PantsCompileOptions {
  private List<String> myTargets = ContainerUtilRt.newArrayList();
  private List<String> myTargetSpecs = ContainerUtilRt.newArrayList();
  private boolean myWithDependees;
  private boolean myLibsWithSources;

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
      ((PantsProjectSettings)receiver).setWithDependees(isWithDependees());
      ((PantsProjectSettings)receiver).setTargetNames(getTargetNames());
      ((PantsProjectSettings)receiver).setLibsWithSources(isLibsWithSources());
    }
  }

  @NotNull
  public List<String> getTargetSpecs() {
    return myTargetSpecs;
  }

  public void setTargetSpecs(List<String> targetSpecs) {
    myTargetSpecs = targetSpecs;
  }

  @NotNull
  @Override
  public List<String> getTargetNames() {
    return myTargets;
  }

  public void setTargetNames(List<String> targets) {
    myTargets = targets;
  }

  public void setWithDependees(boolean withDependees) {
    myWithDependees = withDependees;
  }

  @Override
  public boolean isWithDependees() {
    return myWithDependees;
  }

  public boolean isLibsWithSources() {
    return myLibsWithSources;
  }

  public void setLibsWithSources(boolean libsWithSources) {
    myLibsWithSources = libsWithSources;
  }
}
