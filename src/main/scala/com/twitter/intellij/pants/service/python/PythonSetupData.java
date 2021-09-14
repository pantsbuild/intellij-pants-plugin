// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.python;

import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.serialization.PropertyMapping;
import com.twitter.intellij.pants.service.project.model.PythonInterpreterInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

public class PythonSetupData extends AbstractExternalEntityData {
  private static final long serialVersionUID = 1L;
  @NotNull
  public static final Key<PythonSetupData> KEY =
    Key.create(PythonSetupData.class, ProjectKeys.LIBRARY_DEPENDENCY.getProcessingWeight() + 1);
  private final PythonInterpreterInfo myInterpreterInfo;
  private final ModuleData myOwnerModuleData;

  @PropertyMapping({"myOwnerModuleData", "interpreterInfo"})
  public PythonSetupData(ModuleData ownerModuleData, @NotNull PythonInterpreterInfo interpreterInfo) {
    super(PantsConstants.SYSTEM_ID);
    myOwnerModuleData = ownerModuleData;
    myInterpreterInfo = interpreterInfo;
  }

  public PythonInterpreterInfo getInterpreterInfo() {
    return myInterpreterInfo;
  }

  public ModuleData getOwnerModuleData() {
    return myOwnerModuleData;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj) &&
           getOwnerModuleData().equals(((PythonSetupData)obj).getOwnerModuleData()) &&
           getInterpreterInfo().equals(((PythonSetupData)obj).getInterpreterInfo());
  }
}
