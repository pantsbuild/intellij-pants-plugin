// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.metadata;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TargetMetadata extends AbstractExternalEntityData {
  private static final long serialVersionUID = 1L;
  @NotNull
  public static final Key<TargetMetadata> KEY =
    Key.create(TargetMetadata.class, ProjectKeys.MODULE.getProcessingWeight() + 1);

  private String myModuleName;
  private Set<String> myTargetAddresses;

  public TargetMetadata(ProjectSystemId systemId) {
    super(systemId);
  }

  public String getModuleName() {
    return myModuleName;
  }

  public void setModuleName(String moduleName) {
    myModuleName = moduleName;
  }

  public Set<String> getTargetAddresses() {
    return myTargetAddresses;
  }

  public void setTargetAddresses(Set<String> targetAddresses) {
    myTargetAddresses = targetAddresses;
  }
}
