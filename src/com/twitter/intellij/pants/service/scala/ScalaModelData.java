// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.scala;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Set;

public class ScalaModelData extends AbstractExternalEntityData {
  private static final long serialVersionUID = 1L;
  @NotNull
  public static final Key<ScalaModelData> KEY =
    Key.create(ScalaModelData.class, ProjectKeys.LIBRARY_DEPENDENCY.getProcessingWeight() + 1);
  private Set<File> myScalaCompilerJars;

  public ScalaModelData(ProjectSystemId systemId) {
    super(systemId);
  }


  public void setScalaCompilerJars(Set<File> scalaCompilerJars) {
    myScalaCompilerJars = scalaCompilerJars;
  }

  public Set<File> getScalaCompilerJars() {
    return myScalaCompilerJars;
  }
}
