// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.scala;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.serialization.PropertyMapping;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ScalaModelData extends AbstractExternalEntityData {
  private static final long serialVersionUID = 1L;
  @NotNull
  public static final Key<ScalaModelData> KEY =
    Key.create(ScalaModelData.class, ProjectKeys.LIBRARY_DEPENDENCY.getProcessingWeight() + 1);
  private final String myScalaLibId;
  private final Set<String> myClasspath;

  @PropertyMapping({"myScalaLibId" , "myClasspath"})
  public ScalaModelData(@NotNull String scalaLibId, @NotNull Set<String> classpath) {
    super(PantsConstants.SYSTEM_ID);
    myScalaLibId = scalaLibId;
    myClasspath = classpath;
  }

  @NotNull
  public String getScalaLibId() {
    return myScalaLibId;
  }

  @NotNull
  public Set<String> getClasspath() {
    return myClasspath;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj) && StringUtil.equals(getScalaLibId(), ((ScalaModelData)obj).getScalaLibId());
  }
}
