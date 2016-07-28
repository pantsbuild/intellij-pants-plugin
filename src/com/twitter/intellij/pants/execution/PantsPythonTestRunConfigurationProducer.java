// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.testing.pytest.PyTestUtil;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PantsPythonTestRunConfigurationProducer extends RunConfigurationProducer<ExternalSystemRunConfiguration> {
  protected PantsPythonTestRunConfigurationProducer() {
    super(PantsExternalTaskConfigurationType.getInstance());
  }
  //  TODO: reconfigure to two run config producers that still work just as well (git stash to co old versions)
  @Override
  protected boolean setupConfigurationFromContext(
    @NotNull ExternalSystemRunConfiguration configuration,
    @NotNull ConfigurationContext context,
    @NotNull Ref<PsiElement> sourceElement
  ) {
    final Module module = context.getModule();
    if (module == null) {
      return false;
    }
    final VirtualFile buildRoot = PantsUtil.findBuildRoot(module);
    if (buildRoot == null) {
      return false;
    }
    final List<PantsTargetAddress> targets = PantsUtil.getTargetAddressesFromModule(module);
    if (targets.isEmpty()) {
      return false;
    }
    final PsiElement psiLocation = context.getPsiLocation();
    if (psiLocation == null) {
      return false;
    }

    final ExternalSystemTaskExecutionSettings taskSettings = configuration.getSettings();

    taskSettings.setExternalProjectPath(FileUtil.join(buildRoot.getPath(), targets.iterator().next().getPath()));
    taskSettings.setTaskNames(Collections.singletonList("test"));

    boolean isPython = isOrContainsPyTests(psiLocation);

    if (isPython) {
      if (psiLocation instanceof PyFile) {
        PyFile file = (PyFile) psiLocation;
        buildFromPyTest(file, taskSettings, sourceElement, configuration);
      }
      else if (psiLocation instanceof PsiDirectory) {
        PsiDirectory directory = (PsiDirectory) psiLocation;
        buildFromPyTest(directory, taskSettings, sourceElement, configuration);
      }
    }

    return false;
  }

  private void buildFromPyTest(
    PyFile testFile,
    ExternalSystemTaskExecutionSettings taskSettings,
    Ref<PsiElement> sourceElement,
    ExternalSystemRunConfiguration configuration
  ) {
    sourceElement.set(testFile);
    configuration.setName(testFile.getName());
    System.out.println("set path");
    taskSettings.setExternalProjectPath(testFile.getVirtualFile().getPath());
    taskSettings.setExecutionName(testFile.getName());
    taskSettings.setScriptParameters("");
  }

  private void buildFromPyTest(
    PsiDirectory testDir,
    ExternalSystemTaskExecutionSettings taskSettings,
    Ref<PsiElement> sourceElement,
    ExternalSystemRunConfiguration configuration
  ) {
    sourceElement.set(testDir);
    configuration.setName("Tests " + testDir.getName());
    taskSettings.setExternalProjectPath(testDir.getVirtualFile().getPath());
    taskSettings.setExecutionName(testDir.getName());
    taskSettings.setScriptParameters("");
  }

  @Override
  public boolean isConfigurationFromContext(
    @NotNull ExternalSystemRunConfiguration configuration,
    @NotNull ConfigurationContext context
  ) {
    final ExternalSystemRunConfiguration tempConfig = new ExternalSystemRunConfiguration(
      PantsConstants.SYSTEM_ID, context.getProject(), configuration.getFactory(), configuration.getName()
    );
    final Ref<PsiElement> locationRef = new Ref<PsiElement>(context.getPsiLocation());
    setupConfigurationFromContext(tempConfig, context, locationRef);
    return compareSettings(configuration.getSettings(), tempConfig.getSettings());
  }

  private boolean compareSettings(ExternalSystemTaskExecutionSettings settings1, ExternalSystemTaskExecutionSettings settings2) {
    return settings1.equals(settings2) &&
           StringUtil.equalsIgnoreWhitespaces(settings1.getScriptParameters(), settings2.getScriptParameters()) &&
           StringUtil.equals(settings1.getExecutionName(), settings2.getExecutionName());
  }

  private boolean isOrContainsPyTests(PsiElement element) {
    if (element instanceof PyFile) {
      PyFile pyFile = (PyFile) element;

      for (PyClass pyClass : pyFile.getTopLevelClasses()) {
        if (PyTestUtil.isPyTestClass(pyClass, null)) {
          return true;
        }
      }
    }
    else if (element instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory) element;
      for (PsiFile file : directory.getFiles()) {
        if (isOrContainsPyTests(file)) {
          return true;
        }
      }
    }

    return false;
  }
}
