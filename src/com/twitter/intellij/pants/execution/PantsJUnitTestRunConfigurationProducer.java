// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testIntegration.TestIntegrationUtils;
import com.twitter.intellij.pants.model.IJRC;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class PantsJUnitTestRunConfigurationProducer extends PantsTestRunConfigurationProducer {
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
    final Module module = configurationContext.getModule();
    final VirtualFile buildRoot = configurationContext.getBuildRoot();
    final List<String> targets = configurationContext.getTargets();
    final PsiElement psiLocation = configurationContext.getLocation();
    final ExternalSystemTaskExecutionSettings taskSettings = configuration.getSettings();
    taskSettings.setTaskNames(Collections.singletonList("test"));

    /**
     * Add the module's folder:: to target_roots
     **/
    taskSettings.setExternalProjectPath(FileUtil.join(buildRoot.getPath(), targets.iterator().next()));
    taskSettings.setTaskNames(Collections.singletonList("test"));

    final PsiPackage testPackage;
    // Find out whether the click is on a test package
    if (psiLocation instanceof PsiPackage) {
      testPackage = (PsiPackage) psiLocation;
    }
    else if (psiLocation instanceof PsiDirectory) {
      testPackage = JavaDirectoryService.getInstance().getPackage(((PsiDirectory) psiLocation));
    }
    else {
      testPackage = null;
    }

    // Return false if it is a neither a test class nor a test package
    if (!TestIntegrationUtils.isTest(psiLocation) && !hasJUnitTestClasses(testPackage, module)) {
      return false;
    }

    final List<String> scriptParameters = new ArrayList<>();

    scriptParameters.addAll(targets);

    final PsiClass psiClass = TestIntegrationUtils.findOuterClass(psiLocation);
    final PsiMethod psiMethod = PsiTreeUtil.getParentOfType(psiLocation, PsiMethod.class, false);
    final Optional<String> rcIterate = IJRC.getIteratePantsRc(buildRoot.getPath());
    rcIterate.map(scriptParameters::add);

    if (psiMethod != null) {
      sourceElement.set(psiMethod);
      configuration.setName(psiMethod.getName());
      scriptParameters.add(PantsConstants.PANTS_CLI_OPTION_JUNIT_TEST + "=" + psiClass.getQualifiedName() + "#" + psiMethod.getName());
    }
    else if (psiClass != null) {
      sourceElement.set(psiClass);
      configuration.setName(psiClass.getName());
      scriptParameters.add("--test-junit-test=" + psiClass.getQualifiedName());
    }
    else if (testPackage != null) {
      sourceElement.set(testPackage);
      configuration.setName(testPackage.getName());
      // Iterate through test classes in testPackage that is only in the scope of the module
      Arrays.stream(testPackage.getClasses(module.getModuleScope()))
        .filter(TestIntegrationUtils::isTest)
        .forEach(psiClazz -> scriptParameters.add("--test-junit-test=" + psiClazz.getQualifiedName()));
    }
    else {
      return false;
    }

    taskSettings.setScriptParameters(StringUtil.join(scriptParameters, " "));
    return true;
  }

  private boolean hasJUnitTestClasses(PsiPackage psiPackage, Module module) {
    if (psiPackage == null) return false;
    for (PsiClass psiClass : psiPackage.getClasses(module.getModuleScope())) {
      if (TestIntegrationUtils.isTest(psiClass)) {
        return true;
      }
    }
    return false;
  }
}
