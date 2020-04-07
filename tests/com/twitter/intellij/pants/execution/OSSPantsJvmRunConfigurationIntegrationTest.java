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
import com.intellij.testFramework.MapDataContext;
import com.twitter.intellij.pants.PantsManager;
import com.twitter.intellij.pants.service.task.PantsTaskManager;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;
import com.twitter.intellij.pants.util.ProjectTestJvms;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper.JVM_DEBUG_SETUP_PREFIX;
import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState.BUILD_PROCESS_DEBUGGER_PORT_KEY;


public class OSSPantsJvmRunConfigurationIntegrationTest extends OSSPantsIntegrationTest {
  public void testClassRunConfiguration() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/testjvms");

    PsiClass testClass = ProjectTestJvms.anyTestClass(myProject, getProjectPath());

    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(testClass);

    // Make sure task name is `test` goal.
    String testTask = "test";
    assertEquals(Collections.singletonList(testTask), esc.getSettings().getTaskNames());

    List<String> configScriptParameters = PantsUtil.parseCmdParameters(Optional.ofNullable(esc.getSettings().getScriptParameters()));
    assertContains(configScriptParameters, "--test-junit-test=" + testClass.getQualifiedName());

    // Make sure this configuration does not contain any task before running if added to the project.
    assertEmptyBeforeRunTask(esc);

    /**
     * Check the final command line parameters when config is run, as
     * {@link com.twitter.intellij.pants.service.task.PantsTaskManager} will insert additional parameters post user action.
     */
    Class<? extends ExternalSystemTaskManager<PantsExecutionSettings>> taskManagerClass =
      ExternalSystemManager.EP_NAME.findExtension(PantsManager.class).getTaskManagerClass();
    assertEquals(PantsTaskManager.class, taskManagerClass);

    int debugPort = 5005;
    List<String> debugParameters = Arrays.asList(
      "--no-test-junit-timeouts",
      "test",
      "--jvm-test-junit-options=" + JVM_DEBUG_SETUP_PREFIX + debugPort
    );
    List<String> expectedDebugParameters = Stream.of(debugParameters, configScriptParameters)
      .flatMap(Collection::stream)
      .collect(Collectors.toList());

    GeneralCommandLine finalDebugCommandline = getFinalCommandline(esc, taskManagerClass, OptionalInt.of(debugPort));
    assertEquals(expectedDebugParameters, finalDebugCommandline.getParametersList().getParameters());

    List<String> runParameters = Collections.singletonList("test");
    List<String> expectedRunParameters = Stream.of(runParameters, configScriptParameters)
      .flatMap(Collection::stream)
      .collect(Collectors.toList());

    GeneralCommandLine finalRunCommandline = getFinalCommandline(esc, taskManagerClass, OptionalInt.empty());
    assertEquals(expectedRunParameters, finalRunCommandline.getParametersList().getParameters());
  }

  @NotNull
  private GeneralCommandLine getFinalCommandline(
    ExternalSystemRunConfiguration esc,
    Class<? extends ExternalSystemTaskManager<PantsExecutionSettings>> taskManagerClass,
    OptionalInt debugPort
  ) throws InstantiationException, IllegalAccessException {
    PantsExecutionSettings settings = PantsExecutionSettings.createDefault();
    settings.withVmOptions(PantsUtil.parseCmdParameters(Optional.ofNullable(esc.getSettings().getVmOptions())));
    settings.withArguments(PantsUtil.parseCmdParameters(Optional.ofNullable(esc.getSettings().getScriptParameters())));
    debugPort.ifPresent(port -> settings.putUserData(BUILD_PROCESS_DEBUGGER_PORT_KEY, port));

    GeneralCommandLine commandLine = ((PantsTaskManager) taskManagerClass.newInstance()).constructCommandLine(
      esc.getSettings().getTaskNames(),
      esc.getSettings().getExternalProjectPath(),
      settings
    );
    assertNotNull(commandLine);
    return commandLine;
  }

  public void testMethodRunConfiguration() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/testjvms");

    PsiClass testClass = ProjectTestJvms.testClasses(myProject, getProjectPath())
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Couldn't find a test class"));

    PsiMethod testMethod = Arrays.stream(testClass.getMethods())
      .filter(m -> Arrays.stream(m.getAnnotations()).anyMatch(a -> a.getQualifiedName().equals("org.junit.Test")))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No method annotated with @org.junit.Test"));

    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(testMethod);

    Set<String> items = new HashSet<>(Arrays.asList(esc.getSettings().getScriptParameters().split(" ")));
    assertContains(items, "--test-junit-test=" + testClass.getQualifiedName() + "#" + testMethod.getName());
  }

  public void testModuleRunConfiguration() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/testjvms");

    PsiPackage testPackage = JavaPsiFacade.getInstance(myProject).findPackage("org.pantsbuild.testproject.testjvms");
    assertEquals(1, testPackage.getDirectories().length);

    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(testPackage.getDirectories()[0]);

    Set<String> expectedItems = ProjectTestJvms.testClasses(myProject, getProjectPath())
      .map(aClass -> "--test-junit-test=" + aClass.getQualifiedName())
      .collect(Collectors.toSet());
    assertNotEmpty(expectedItems);

    Set<String> items = new HashSet<>(Arrays.asList(esc.getSettings().getScriptParameters().split(" ")));
    assertContains(items, expectedItems);
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
    for (RunConfigurationProducer producer : producers) {
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

  private void assertContains(Collection<String> collection, String expected) {
    assertContains(collection, Collections.singleton(expected));
  }

  private void assertContains(Collection<String> collection, Collection<String> expectedElements) {
    Set<String> missing = expectedElements.stream()
      .filter(expected -> !collection.contains(expected))
      .collect(Collectors.toSet());

    if (!missing.isEmpty()) {
      String actual = collection.stream().collect(Collectors.joining(",", "[", "]"));
      String expected = missing.stream().collect(Collectors.joining(",", "[", "]"));
      String message = "Elements missing from " + actual + ": " + expected;
      Assert.fail(message);
    }
  }
}
