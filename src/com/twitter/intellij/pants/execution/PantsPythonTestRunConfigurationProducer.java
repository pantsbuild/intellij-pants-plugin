// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ThreeState;
import com.jetbrains.python.codeInsight.testIntegration.PyTestFinder;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.testing.PythonUnitTestDetectorsBasedOnSettings;
import com.twitter.intellij.pants.model.IJRC;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PantsPythonTestRunConfigurationProducer extends PantsTestRunConfigurationProducer {

  @Override
  protected boolean setupConfigurationFromContext(
    @NotNull ExternalSystemRunConfiguration configuration,
    @NotNull ConfigurationContext context,
    @NotNull Ref<PsiElement> sourceElement
  ) {
    Optional<PantsConfigurationContext> contextOptional = PantsConfigurationContext.validatesAndCreate(context);
    if (!contextOptional.isPresent()) {
      return false;
    }

    PantsConfigurationContext configurationContext = contextOptional.get();
    final List<String> targets = configurationContext.getTargets();
    final PsiElement psiLocation = configurationContext.getLocation();
    final ExternalSystemTaskExecutionSettings taskSettings = configuration.getSettings();
    taskSettings.setTaskNames(Collections.singletonList("test"));

    boolean isPython = isOrContainsPyTests(psiLocation);
    if (!isPython) {
      return false;
    }
    if (psiLocation instanceof PyFile) {
      PyFile file = (PyFile) psiLocation;
      return buildFromPyTest(file, file.getName(), file.getVirtualFile().getPath(), targets, taskSettings, sourceElement, configuration);
    }
    else if (psiLocation instanceof PsiDirectory) {
      PsiDirectory dir = (PsiDirectory) psiLocation;
      return buildFromPyTest(dir, dir.getName(), dir.getVirtualFile().getPath(), targets, taskSettings, sourceElement, configuration);
    }
    else {
      PyFile file = (PyFile) psiLocation.getContainingFile();
      PyFunction pyFunction = PsiTreeUtil.getParentOfType(psiLocation, PyFunction.class, false);
      PyClass pyClass = PsiTreeUtil.getParentOfType(psiLocation, PyClass.class, false);
      if (pyFunction != null) {
        return buildFromPyTest(
          psiLocation,
          "'" + pyClass.getName() + " and " + pyFunction.getName() + "'",
          file.getVirtualFile().getPath(),
          targets,
          taskSettings,
          sourceElement,
          configuration
        );
      }
      if (pyClass != null) {
        return buildFromPyTest(
          psiLocation, pyClass.getName(), file.getVirtualFile().getPath(), targets, taskSettings, sourceElement, configuration);
      }
      return buildFromPyTest(
        psiLocation,
        file.getName(),
        file.getVirtualFile().getPath(),
        targets,
        taskSettings,
        sourceElement,
        configuration
      );
    }
  }

  private boolean buildFromPyTest(
    PsiElement testElem,
    String elemStr,
    String path,
    List<String> targets,
    ExternalSystemTaskExecutionSettings taskSettings,
    Ref<PsiElement> sourceElement,
    ExternalSystemRunConfiguration configuration
  ) {
    sourceElement.set(testElem);
    configuration.setName("Pants tests in " + elemStr);
    taskSettings.setExternalProjectPath(path);
    String scriptParams = StringUtil.join(targets, " ");
    scriptParams += " " + PantsConstants.PANTS_CLI_OPTION_PYTEST + "=\"-k " + elemStr + "\"";
    final Optional<String> rcIterate = IJRC.getIteratePantsRc(path);
    scriptParams += rcIterate.orElse("");

    taskSettings.setExecutionName(elemStr);
    taskSettings.setScriptParameters(scriptParams);
    return true;
  }

  private boolean isOrContainsPyTests(PsiElement element) {
    if (new PyTestFinder().isTest(element)) {
      return true;
    }

    if (element instanceof PyFile) {
      PyFile pyFile = (PyFile) element;

      for (PyClass pyClass : pyFile.getTopLevelClasses()) {
        if (PythonUnitTestDetectorsBasedOnSettings.isTestClass(pyClass, ThreeState.YES, null)) {
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
