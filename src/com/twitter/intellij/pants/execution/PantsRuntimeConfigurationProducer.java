// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.openapi.externalSystem.service.execution.AbstractExternalSystemRuntimeConfigurationProducer;
import org.jetbrains.annotations.NotNull;

public class PantsRuntimeConfigurationProducer extends AbstractExternalSystemRuntimeConfigurationProducer {

  public PantsRuntimeConfigurationProducer(@NotNull PantsExternalTaskConfigurationType type) {
    super(type);
  }
}