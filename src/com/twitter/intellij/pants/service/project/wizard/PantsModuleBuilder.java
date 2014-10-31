// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;

public class PantsModuleBuilder extends AbstractExternalModuleBuilder<PantsProjectSettings> {
  protected PantsModuleBuilder() {
    super(PantsConstants.SYSTEM_ID, new PantsProjectSettings());
  }

  @Override
  public void setupRootModel(ModifiableRootModel modifiableRootModel) throws ConfigurationException {

  }

  @Override
  public boolean isSuitableSdkType(SdkTypeId sdk) {
    return true;
  }

  @Override
  public ModuleType getModuleType() {
    return StdModuleTypes.JAVA;
  }
}
