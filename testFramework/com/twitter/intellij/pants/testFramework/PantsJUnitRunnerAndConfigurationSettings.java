// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationPerRunnerSettings;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.util.Factory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsJUnitRunnerAndConfigurationSettings implements RunnerAndConfigurationSettings {

  private boolean myTemporary;
  private String myName = "junitTest";
  private boolean myEditBeforeRun;
  private boolean myActivateToolWindowBeforeRun;
  private boolean mySingleton;
  private String myFolderName;

  private final RunConfiguration myRunConfiguration;

  public PantsJUnitRunnerAndConfigurationSettings(RunConfiguration configuration) {
    myRunConfiguration = configuration;
  }

  @NotNull
  @Override
  public ConfigurationType getType() {
    return JUnitConfigurationType.getInstance();
  }

  @Override
  public RunConfiguration getConfiguration() {
    return myRunConfiguration;
  }

  @Nullable
  @Override
  public ConfigurationFactory getFactory() {
    return null;
  }

  @Override
  public boolean isTemplate() {
    return false;
  }

  @Override
  public boolean isTemporary() {
    return myTemporary;
  }

  @Override
  public void setTemporary(boolean temporary) {
    myTemporary = temporary;
  }

  @Override
  public void setName(String name) {
    myName = name;
  }

  @Override
  @NotNull
  public String getName() {
    return myName;
  }

  @Override
  public String getUniqueID() {
    return getName();
  }

  @Nullable
  @Override
  public RunnerSettings getRunnerSettings(@NotNull ProgramRunner runner) {
    return null;
  }

  @Nullable
  @Override
  public ConfigurationPerRunnerSettings getConfigurationSettings(@NotNull ProgramRunner runner) {
    return null;
  }

  @Override
  public void checkSettings() throws RuntimeConfigurationException {
    myRunConfiguration.checkConfiguration();
  }

  @Override
  public void checkSettings(@Nullable Executor executor) throws RuntimeConfigurationException {
    myRunConfiguration.checkConfiguration();
  }

  @Override
  public boolean canRunOn(@NotNull ExecutionTarget target) {
    return true;
  }

  @Override
  public Factory<RunnerAndConfigurationSettings> createFactory() {
    return new Factory<RunnerAndConfigurationSettings>() {
      @Override
      public RunnerAndConfigurationSettings create() {
        return new PantsJUnitRunnerAndConfigurationSettings(myRunConfiguration);
      }
    };
  }

  @Override
  public void setEditBeforeRun(boolean editBeforeRun) {
    myEditBeforeRun = editBeforeRun;
  }

  @Override
  public boolean isEditBeforeRun() {
    return myEditBeforeRun;
  }

  @Override
  public void setActivateToolWindowBeforeRun(boolean activateToolWindowBeforeRun) {
    myActivateToolWindowBeforeRun = activateToolWindowBeforeRun;
  }

  @Override
  public boolean isActivateToolWindowBeforeRun() {
    return myActivateToolWindowBeforeRun;
  }

  @Override
  public void setSingleton(boolean singleton) {
    mySingleton = singleton;
  }

  @Override
  public boolean isSingleton() {
    return mySingleton;
  }

  @Override
  public void setFolderName(@Nullable String folderName) {
    myFolderName = folderName;
  }

  @Nullable
  @Override
  public String getFolderName() {
    return myFolderName;
  }
}
