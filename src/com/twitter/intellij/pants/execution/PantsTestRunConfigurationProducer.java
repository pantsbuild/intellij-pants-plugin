// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testIntegration.TestIntegrationUtils;
import com.intellij.util.ObjectUtils;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collections;
import java.util.List;

import com.intellij.util.Function;

public class PantsTestRunConfigurationProducer extends RunConfigurationProducer<ExternalSystemRunConfiguration> {
  protected PantsTestRunConfigurationProducer() {
    super(PantsExternalTaskConfigurationType.getInstance());
  }

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
    final VirtualFile workingDir = PantsUtil.findPantsWorkingDir(module);
    if (workingDir == null) {
      return false;
    }
    final List<PantsTargetAddress> targets = PantsUtil.getTargetAddressesFromModule(module);
    if (targets.isEmpty()) {
      return false;
    }

    final ExternalSystemTaskExecutionSettings taskSettings = configuration.getSettings();

    /**
     * Add the module's folder:: to target_roots
     **/
    taskSettings.setExternalProjectPath(FileUtil.join(workingDir.getPath(), targets.iterator().next().getPath()));
    taskSettings.setTaskNames(Collections.singletonList("test"));

    final PsiElement psiLocation = context.getPsiLocation();
    final PsiPackage testPackage;
    if (psiLocation instanceof PsiPackage) {
      testPackage = (PsiPackage)psiLocation;
    }
    else if (psiLocation instanceof PsiDirectory) {
      testPackage = JavaDirectoryService.getInstance().getPackage(((PsiDirectory)psiLocation));
    }
    else {
      testPackage = null;
    }

    if (testPackage == null && (psiLocation == null || !TestIntegrationUtils.isTest(psiLocation))) {
      return false;
    }

    final PsiClass psiClass = TestIntegrationUtils.findOuterClass(psiLocation);

    if (psiClass != null) {
      // Try to determine whether psiLocation is in a method
      PsiMethod psiMethod = PsiTreeUtil.getParentOfType(context.getPsiLocation(), PsiMethod.class, false);
      String testFullyQualifiedName = psiClass.getQualifiedName();
      if (psiMethod != null) {
        sourceElement.set(psiMethod);
        testFullyQualifiedName += "#" + psiMethod.getName();
        configuration.setName(psiMethod.getName());
      }
      else {
        sourceElement.set(psiClass);
        configuration.setName(psiClass.getName());
      }

      taskSettings.setScriptParameters(
        "--test-junit-test=" + testFullyQualifiedName
      );
    }
    else if (testPackage != null) {
      sourceElement.set(testPackage);
      configuration.setName("Tests " + testPackage.getName());

      String junitTestArgs = "";
      // Iterate through test classes in testPackage that is only in the scope of the module
      for (PsiClass clazz : testPackage.getClasses(module.getModuleScope())) {
        if (TestIntegrationUtils.isTest(clazz)) {
          junitTestArgs += " --test-junit-test=" + clazz.getQualifiedName();
        }
      }

      taskSettings.setScriptParameters(
        junitTestArgs
      );
    }
    else {
      return false;
      //final String name = targets.size() == 1 ? targetAddress.getTargetName() : module.getName();
      //configuration.setName("Test " + name);
    }

    return true;
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
    if (!settings1.equals(settings2)) {
      return false;
    }

    if (!StringUtil.equalsIgnoreWhitespaces(settings1.getScriptParameters(), settings2.getScriptParameters())) {
      return false;
    }

    return true;
  }
}
