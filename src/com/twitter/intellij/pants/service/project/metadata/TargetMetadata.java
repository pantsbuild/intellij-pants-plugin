// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.metadata;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TargetMetadata extends AbstractExternalEntityData {
  private static final long serialVersionUID = 1L;
  @NotNull
  public static final Key<TargetMetadata> KEY =
    Key.create(TargetMetadata.class, ProjectKeys.MODULE.getProcessingWeight() + 1);

  private final String myModuleName;
  private Set<String> myLibraryExcludes = Collections.emptySet();
  private Set<String> myTargetAddresses = Collections.emptySet();
  private Set<TargetAddressInfo> myTargetAddressInfoSet = Collections.emptySet();

  public TargetMetadata(ProjectSystemId systemId, @NotNull String moduleName) {
    super(systemId);
    myModuleName = moduleName;
  }

  @NotNull
  public String getModuleName() {
    return myModuleName;
  }

  @NotNull
  public Set<String> getTargetAddresses() {
    return myTargetAddresses;
  }

  public void setTargetAddresses(Collection<String> targetAddresses) {
    myTargetAddresses = new HashSet<String>(targetAddresses);
  }

  public Set<TargetAddressInfo> getTargetAddressInfoSet() {
    return myTargetAddressInfoSet;
  }

  public void setTargetAddressInfoSet(Set<TargetAddressInfo> targetAddressInfoSet) {
    myTargetAddressInfoSet = new HashSet<TargetAddressInfo>(targetAddressInfoSet);
  }

  @NotNull
  public Set<String> getLibraryExcludes() {
    return myLibraryExcludes;
  }

  public void setLibraryExcludes(Set<String> libraryExcludes) {
    myLibraryExcludes = libraryExcludes;
  }
}
