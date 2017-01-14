// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.util.containers.ContainerUtilRt;
import com.twitter.intellij.pants.model.PantsCompileOptions;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PantsProjectSettings extends ExternalProjectSettings implements PantsCompileOptions {
  private List<String> myTargetSpecs = ContainerUtilRt.newArrayList();
  private boolean myLibsWithSources;
  private boolean myEnableIncrementalImport;
  /**
   * @param targetSpecs targets explicted listed from `pants idea-plugin` goal.
   * @param externalProjectPath path to the Pants project.
   * @param libsWithSources whether to import sources and docs when resolving for jars.
   */
  public PantsProjectSettings(
    List<String> targetSpecs,
    String externalProjectPath,
    boolean libsWithSources,
    boolean isEnableIncrementalImport
  ) {
    myTargetSpecs = targetSpecs;
    myLibsWithSources = libsWithSources;
    setExternalProjectPath(externalProjectPath);
    myEnableIncrementalImport = isEnableIncrementalImport;
  }

  public PantsProjectSettings() {
  }

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
      ((PantsProjectSettings)receiver).setLibsWithSources(isLibsWithSources());
      ((PantsProjectSettings)receiver).setTargetSpecs(getTargetSpecs());
      ((PantsProjectSettings)receiver).setEnableIncrementalImport(isEnableIncrementalImport());
    }
  }

  /**
   * Get the target specs used to launched `pants idea-plugin`.
   */
  @NotNull
  public List<String> getTargetSpecs() {
    return myTargetSpecs;
  }

  public void setTargetSpecs(List<String> targetSpecs) {
    myTargetSpecs = targetSpecs;
  }

  public boolean isLibsWithSources() {
    return myLibsWithSources;
  }

  public void setLibsWithSources(boolean libsWithSources) {
    myLibsWithSources = libsWithSources;
  }

  public boolean isEnableIncrementalImport() {
    return myEnableIncrementalImport;
  }

  public void setEnableIncrementalImport(boolean enableIncrementalImport) {
    myEnableIncrementalImport = enableIncrementalImport;
  }
}
