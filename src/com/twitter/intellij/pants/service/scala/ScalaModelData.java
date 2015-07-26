// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.scala;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

public class ScalaModelData extends AbstractExternalEntityData {
  private static final long serialVersionUID = 1L;
  @NotNull
  public static final Key<ScalaModelData> KEY =
    Key.create(ScalaModelData.class, ProjectKeys.LIBRARY_DEPENDENCY.getProcessingWeight() + 1);
  private final String myScalaLibId;

  public ScalaModelData(@NotNull String scalaLibId) {
    super(PantsConstants.SYSTEM_ID);
    myScalaLibId = scalaLibId;
  }

  @NotNull
  public String getScalaLibId() {
    return myScalaLibId;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj) && StringUtil.equals(getScalaLibId(), ((ScalaModelData)obj).getScalaLibId());
  }
}
