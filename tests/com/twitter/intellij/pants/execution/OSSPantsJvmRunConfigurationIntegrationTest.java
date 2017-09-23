// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.MapDataContext;
import com.twitter.intellij.pants.PantsManager;
import com.twitter.intellij.pants.service.task.PantsTaskManager;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


public class OSSPantsJvmRunConfigurationIntegrationTest extends OSSPantsIntegrationTest {
  public void testClassRunConfiguration() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/testjvms");

    String classReference = "org.pantsbuild.testproject.testjvms.TestSix";

    PsiClass testClass = JavaPsiFacade.getInstance(myProject).findClass(classReference, GlobalSearchScope.allScope(myProject));
    assertNotNull(testClass);

    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(testClass);

    // Make sure task name is `test` goal.
    assertEquals(Collections.singletonList("test"), esc.getSettings().getTaskNames());

    List<String> configScriptParameters = PantsUtil.parseCmdParameters(Optional.ofNullable(esc.getSettings().getScriptParameters()));

    List<String> expectedConfigScriptParameters = Arrays.asList(
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:eight-test-platform",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:six",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:seven",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:eight",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:base",
      "--test-junit-test=" + classReference
    );
    assertEquals(expectedConfigScriptParameters, configScriptParameters);

    // Make sure this configuration does not contain any task before running if added to the project.
    assertEmptyBeforeRunTask(esc);

    /**
     * Check the final command line parameters when config is run, as
     * {@link com.twitter.intellij.pants.service.task.PantsTaskManager} will insert additional parameters post user action.
     */
    Class<? extends ExternalSystemTaskManager<PantsExecutionSettings>> taskManagerClass =
      ExternalSystemManager.EP_NAME.findExtension(PantsManager.class).getTaskManagerClass();
    assertEquals(PantsTaskManager.class, taskManagerClass);

    String debuggerSetup = "dummy_debugger_setup";

    GeneralCommandLine finalDebugCommandline = getFinalCommandline(esc, debuggerSetup, taskManagerClass);

    List<String> expectedFinalDebugCommandlineParameters = Arrays.asList(
      "--no-test-junit-timeouts",
      "--jvm-test-junit-options=" + debuggerSetup,
      "test",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:eight-test-platform",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:six",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:seven",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:eight",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:base",
      "--test-junit-test=" + classReference
    );

    assertEquals(expectedFinalDebugCommandlineParameters, finalDebugCommandline.getParametersList().getParameters());

    GeneralCommandLine finalRunCommandline = getFinalCommandline(esc, null, taskManagerClass);

    List<String> expectedFinalRunCommandlineParameters = Arrays.asList(
      "test",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:eight-test-platform",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:six",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:seven",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:eight",
      "testprojects/tests/java/org/pantsbuild/testproject/testjvms:base",
      "--test-junit-test=" + classReference
    );
    assertEquals(expectedFinalRunCommandlineParameters, finalRunCommandline.getParametersList().getParameters());
  }

  @NotNull
  private GeneralCommandLine getFinalCommandline(
    ExternalSystemRunConfiguration esc,
    String debuggerSetup,
    Class<? extends ExternalSystemTaskManager<PantsExecutionSettings>> taskManagerClass
  ) throws InstantiationException, IllegalAccessException {
    GeneralCommandLine commandLine = ((PantsTaskManager) taskManagerClass.newInstance()).constructCommandLine(
      esc.getSettings().getTaskNames(),
      esc.getSettings().getExternalProjectPath(),
      PantsExecutionSettings.createDefault(),
      PantsUtil.parseCmdParameters(Optional.ofNullable(esc.getSettings().getVmOptions())),
      PantsUtil.parseCmdParameters(Optional.ofNullable(esc.getSettings().getScriptParameters())),
      debuggerSetup
    );
    assertNotNull(commandLine);
    return commandLine;
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
    assertNotNull(psiElement);
    ConfigurationContext context = createContext(psiElement, new MapDataContext());
    assertNotNull(context);
    ConfigurationFromContext myPantsConfigurationFromContext = getPantsJunitConfigurationFromContext(context);
    assertNotNull(myPantsConfigurationFromContext);
    return (ExternalSystemRunConfiguration) myPantsConfigurationFromContext.getConfiguration();
  }

  private ConfigurationFromContext getPantsJunitConfigurationFromContext(ConfigurationContext context) {
    assertNotNull(context);
    List<RunConfigurationProducer<?>> producers = RunConfigurationProducer.getProducers(myProject);
    assertTrue(producers.size() > 0);
    System.out.println(Integer.toString(producers.size()));
    for (RunConfigurationProducer producer : producers) {
      System.out.println(producer.toString());
      if (producer instanceof PantsJUnitTestRunConfigurationProducer) {
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
