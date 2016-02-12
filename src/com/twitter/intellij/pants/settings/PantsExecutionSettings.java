// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings;
import com.twitter.intellij.pants.model.PantsExecutionOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PantsExecutionSettings extends ExternalSystemExecutionSettings implements PantsExecutionOptions {
  private final boolean myWithDependees;
  private final boolean myLibsWithSourcesAndDocs;


  private final boolean myUseIdeaProjectJdk;
  private List<String> myTargetNames;

  public PantsExecutionSettings() {
    this(Collections.<String>emptyList(), false, true, false);
  }

  public PantsExecutionSettings(List<String> targetNames, boolean withDependees, boolean libsWithSourcesAndDocs, boolean useIdeaProjectJdk) {
    myTargetNames = targetNames;
    myWithDependees = withDependees;
    myLibsWithSourcesAndDocs = libsWithSourcesAndDocs;
    myUseIdeaProjectJdk = useIdeaProjectJdk;
  }

  @NotNull
  public List<String> getTargetNames() {
    return myTargetNames;
  }

  @Override
  public boolean isWithDependees() {
    return myWithDependees;
  }

  public boolean isLibsWithSourcesAndDocs() {
    return myLibsWithSourcesAndDocs;
  }

  public boolean isUseIdeaProjectJdk() {
    return myUseIdeaProjectJdk;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    PantsExecutionSettings settings = (PantsExecutionSettings)o;
    if (myUseIdeaProjectJdk != settings.myUseIdeaProjectJdk) return false;
    if (myWithDependees != settings.myWithDependees) return false;
    if (myLibsWithSourcesAndDocs != settings.myLibsWithSourcesAndDocs) return false;
    if (myTargetNames != null ? !myTargetNames.equals(settings.myTargetNames) : settings.myTargetNames != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (myWithDependees ? 1 : 0);
    result = 31 * result + (myTargetNames != null ? myTargetNames.hashCode() : 0);
    result = 31 * result + (myLibsWithSourcesAndDocs ? 1 : 0);
    result = 31 * result + (myUseIdeaProjectJdk ? 1 : 0);
    return result;
  }
}
