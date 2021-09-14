// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

public class PantsExternalTaskConfigurationType extends AbstractExternalSystemTaskConfigurationType {

  public PantsExternalTaskConfigurationType() {
    super(PantsConstants.SYSTEM_ID);
  }

  public static PantsExternalTaskConfigurationType getInstance() {
    return (PantsExternalTaskConfigurationType)ExternalSystemUtil.findConfigurationType(PantsConstants.SYSTEM_ID);
  }

  @NotNull
  @Override
  protected String getConfigurationFactoryId() {
    return getExternalSystemId().getId();
  }
}
