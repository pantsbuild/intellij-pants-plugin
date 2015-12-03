// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.testIntegration.TestIntegrationUtils;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.PsiMethod;

import java.util.Collections;
import java.util.List;

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
     * todo: try to find a better way to handle multiple targets per external project
     * it's a big issue for targets with common source roots
     * but it's also a sign of a bad targets layout.
     * For now we'll try to find a `main` target or a target with 'test' in it's name.
     *
     * Note: there is additional issue with PantsTaskManager
     * PantsTaskManager#executeTasks supposed to be invoked in an external process
     * so there is no access to ProjectManager#getOpenProjects() for example.
    **/
    PantsTargetAddress mainTarget = null, testTarget = null;
    for (PantsTargetAddress target : targets) {
      if (target.isMainTarget()) {
        mainTarget = target;
        break;
      }
      if (StringUtil.startsWith(target.getTargetName(), "test")) {
        testTarget = target;
      }
    }
    final PantsTargetAddress targetAddress =
      ObjectUtils.notNull(mainTarget, ObjectUtils.notNull(testTarget, targets.iterator().next()));

    taskSettings.setExternalProjectPath(FileUtil.join(workingDir.getPath(), targetAddress.toString()));
    taskSettings.setTaskNames(Collections.singletonList("test"));

    final PsiElement psiLocation = context.getPsiLocation();

    if (psiLocation == null || !TestIntegrationUtils.isTest(psiLocation)) {
      return false;
    }

    final PsiClass psiClass = TestIntegrationUtils.findOuterClass(psiLocation);

    if (psiClass != null) {
      PsiMethod psiMethod = null;
      // Set sourceElement to be the PsiMethod if the right click content matches any class method name
      for (PsiMethod method : psiClass.getMethods()) {
        if (method.getName() == psiLocation.getText()) {
          psiMethod = method;
          break;
        }
      }
      String testFullyQualfiedName = psiClass.getQualifiedName();
      if (psiMethod != null) {
        sourceElement.set(psiMethod);
        testFullyQualfiedName += "#" + psiMethod.getName();
        configuration.setName(psiMethod.getName());
      }
      else {
        sourceElement.set(psiClass);
        configuration.setName(psiClass.getName());
      }

      taskSettings.setScriptParameters(
        "--no-test-junit-suppress-output " +
        "--test-junit-test=" + testFullyQualfiedName
      );
    }
    else {
      final String name = targets.size() == 1 ? targetAddress.getTargetName() : module.getName();
      configuration.setName("Test " + name);
      taskSettings.setScriptParameters("--no-test-junit-suppress-output");
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
