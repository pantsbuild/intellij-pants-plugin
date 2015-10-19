// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.intellij.util.containers.ContainerUtilRt;
import com.twitter.intellij.pants.model.PantsExecutionOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PantsExecutionSettings extends ExternalSystemExecutionSettings implements PantsExecutionOptions {
  private final boolean myWithDependees;
  private final boolean myCompileWithIntellij;
  private final boolean myCompileWithDebugInfo;
  private final boolean myLibsWithSources;
  private List<String> myTargetNames;

  public PantsExecutionSettings() {
    this(Collections.<String>emptyList(), false, false, false, true);
  }

  public PantsExecutionSettings(List<String> targetNames, boolean withDependees, boolean compileWithIntellij, boolean compileWithDebugInfo, boolean libsWithSources) {
    myTargetNames = targetNames;
    myWithDependees = withDependees;
    myCompileWithIntellij = compileWithIntellij;
    myCompileWithDebugInfo = compileWithDebugInfo;
    myLibsWithSources = libsWithSources;

  }

  @NotNull
  public List<String> getTargetNames() {
    return myTargetNames;
  }

  @Override
  public boolean isWithDependees() {
    return myWithDependees;
  }

  public boolean isCompileWithIntellij() {
    return myCompileWithIntellij;
  }

  public boolean isCompileWithDebugInfo() {return myCompileWithDebugInfo; }

  public boolean isLibsWithSources() {
    return myLibsWithSources;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    PantsExecutionSettings settings = (PantsExecutionSettings)o;

    if (myWithDependees != settings.myWithDependees) return false;
    if (myCompileWithIntellij != settings.myCompileWithIntellij) return false;
    if (myLibsWithSources != settings.myLibsWithSources) return false;
    if (myTargetNames != null ? !myTargetNames.equals(settings.myTargetNames) : settings.myTargetNames != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (myWithDependees ? 1 : 0);
    result = 31 * result + (myTargetNames != null ? myTargetNames.hashCode() : 0);
    result = 31 * result + (myCompileWithIntellij ? 1 : 0);
    result = 31 * result + (myLibsWithSources ? 1 : 0);
    return result;
  }
}
