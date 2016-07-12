// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.MapDataContext;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class OSSPantsExamplesRunConfigurationIntegrationTest extends OSSPantsIntegrationTest {
  public void testClassRunConfiguration() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/testjvms");

    String classReference = "org.pantsbuild.testproject.testjvms.TestSix";
    String expectedJunitTestOption = "--test-junit-test=" + classReference;

    PsiClass testClass = JavaPsiFacade.getInstance(myProject).findClass(classReference, GlobalSearchScope.allScope(myProject));
    assertNotNull(testClass);

    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(testClass);


    // Make sure task name is `test` goal.
    assertEquals(Collections.singleton("test"), esc.getSettings().getTaskNames());

    // `scriptParameters` will return something like:
    // testprojects/tests/java/org/pantsbuild/testproject/testjvms:base
    // [some more targets]
    // --test-junit-test=org.pantsbuild.testproject.testjvms.TestSix"
    // [some more options]
    List<String> scriptParameters = Arrays.asList(esc.getSettings().getScriptParameters().split(" "));
    List<String> targetAddresses = PantsUtil.getNonGenTargetAddressFromModule(ModuleUtil.findModuleForPsiElement(testClass));



    // Make sure parameters contains all target addresses in module.
    assertTrue(scriptParameters.containsAll(targetAddresses));

    assertEquals(
      String.format("%s is either not in the script parameters or not the last element", expectedJunitTestOption),
      Math.max(0, scriptParameters.size() - 1), scriptParameters.indexOf(expectedJunitTestOption)
    );

    // Make sure this configuration does not contain any task before running if added to the project.
    assertEmptyBeforeRunTask(esc);
  }

  public void testMethodRunConfiguration() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/testjvms");

    String classReference = "org.pantsbuild.testproject.testjvms.TestSix";
    String methodName = "testSix";

    PsiClass testClass = JavaPsiFacade.getInstance(myProject).findClass(classReference, GlobalSearchScope.allScope(myProject));
    assertNotNull(testClass);
    PsiMethod[] testMethods = testClass.findMethodsByName(methodName, false);
    assertEquals(testMethods.length, 1);
    PsiMethod testMethod = testMethods[0];
    assertNotNull(testMethod);

    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(testMethod);

    Set<String> items = new HashSet<String>(Arrays.asList(esc.getSettings().getScriptParameters().split(" ")));
    assertTrue(items.contains("--test-junit-test=" + classReference + "#" + methodName));
  }

  public void testModuleRunConfiguration() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/testjvms");

    PsiPackage testPackage = JavaPsiFacade.getInstance(myProject).findPackage("org.pantsbuild.testproject.testjvms");
    assertEquals(1, testPackage.getDirectories().length);

    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(testPackage.getDirectories()[0]);

    Set<String> items = new HashSet<>(Arrays.asList(esc.getSettings().getScriptParameters().split(" ")));
    assertTrue(items.contains("--test-junit-test=org.pantsbuild.testproject.testjvms.TestSix"));
    assertTrue(items.contains("--test-junit-test=org.pantsbuild.testproject.testjvms.TestSeven"));
    assertTrue(items.contains("--test-junit-test=org.pantsbuild.testproject.testjvms.TestEight"));
  }

  @NotNull
  private ExternalSystemRunConfiguration getExternalSystemRunConfiguration(PsiElement psiElement) {
    ConfigurationContext context = createContext(psiElement, new MapDataContext());
    ConfigurationFromContext myPantsConfigurationFromContext = getPantsConfigurationFromContext(context);
    return (ExternalSystemRunConfiguration) myPantsConfigurationFromContext.getConfiguration();
  }

  private ConfigurationFromContext getPantsConfigurationFromContext(ConfigurationContext context) {
    for (RunConfigurationProducer producer : RunConfigurationProducer.getProducers(myProject)) {
      if (producer instanceof PantsTestRunConfigurationProducer) {
        return producer.createConfigurationFromContext(context);
      }
    }
    return null;
  }

  private ConfigurationContext createContext(@NotNull PsiElement psiClass, @NotNull MapDataContext dataContext) {
    dataContext.put(CommonDataKeys.PROJECT, myProject);
    if (LangDataKeys.MODULE.getData(dataContext) == null) {
      dataContext.put(LangDataKeys.MODULE, ModuleUtilCore.findModuleForPsiElement(psiClass));
    }
    dataContext.put(Location.DATA_KEY, PsiLocation.fromPsiElement(psiClass));
    return ConfigurationContext.getFromContext(dataContext);
  }
}
