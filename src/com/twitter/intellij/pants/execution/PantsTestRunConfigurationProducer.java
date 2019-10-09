// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

public abstract class PantsTestRunConfigurationProducer extends LazyRunConfigurationProducer<ExternalSystemRunConfiguration> {

  @NotNull
  @Override
  public ConfigurationFactory getConfigurationFactory() {
    return PantsExternalTaskConfigurationType.getInstance().getFactory();
  }

  @Override
  protected abstract boolean setupConfigurationFromContext(
    @NotNull ExternalSystemRunConfiguration configuration,
    @NotNull ConfigurationContext context,
    @NotNull Ref<PsiElement> sourceElement
  );

  @Override
  public boolean isConfigurationFromContext(
    @NotNull ExternalSystemRunConfiguration configuration,
    @NotNull ConfigurationContext context
  ) {
    final ExternalSystemRunConfiguration tempConfig = new ExternalSystemRunConfiguration(
      PantsConstants.SYSTEM_ID, context.getProject(), configuration.getFactory(), configuration.getName()
    );
    final Ref<PsiElement> locationRef = new Ref<>(context.getPsiLocation());
    setupConfigurationFromContext(tempConfig, context, locationRef);
    return compareSettings(configuration.getSettings(), tempConfig.getSettings());
  }

  private boolean compareSettings(ExternalSystemTaskExecutionSettings settings1, ExternalSystemTaskExecutionSettings settings2) {
    return settings1.equals(settings2) &&
           StringUtil.equalsIgnoreWhitespaces(settings1.getScriptParameters(), settings2.getScriptParameters()) &&
           StringUtil.equals(settings1.getExecutionName(), settings2.getExecutionName());
  }

}
