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
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.MapDataContext;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.impl.PyClassImpl;
import com.jetbrains.python.psi.impl.PyFileImpl;
import com.jetbrains.python.psi.stubs.PyClassNameIndex;
import com.jetbrains.python.psi.types.PyClassLikeType;
import com.jetbrains.python.psi.types.PyClassTypeImpl;
import com.jetbrains.python.psi.types.TypeEvalContext;
import com.jetbrains.python.testing.pytest.PyTestUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class OSSPantsExamplesRunConfigurationIntegrationTest extends OSSPantsIntegrationTest {
  public void testClassRunConfiguration() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/testjvms");

    String classReference = "org.pantsbuild.testproject.testjvms.TestSix";
    PsiClass testClass = JavaPsiFacade.getInstance(myProject).findClass(classReference, GlobalSearchScope.allScope(myProject));
    assertNotNull(testClass);

    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(testClass);

    // esc.getSettings().getScriptParameters() will return something like:
    // "--no-colors test --test-junit-test=org.pantsbuild.testproject.testjvms.TestSix"
    Set<String> items = new HashSet<String>(Arrays.asList(esc.getSettings().getScriptParameters().split(" ")));
    assertTrue(items.contains("--test-junit-test=" + classReference));
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
    assertEquals(testPackage.getDirectories().length, 1);

    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(testPackage.getDirectories()[0]);

    Set<String> items = new HashSet<String>(Arrays.asList(esc.getSettings().getScriptParameters().split(" ")));
    assertTrue(items.contains("--test-junit-test=org.pantsbuild.testproject.testjvms.TestSix"));
    assertTrue(items.contains("--test-junit-test=org.pantsbuild.testproject.testjvms.TestSeven"));
    assertTrue(items.contains("--test-junit-test=org.pantsbuild.testproject.testjvms.TestEight"));
  }

  public void testPyTestRunConfiguration() throws Throwable {
    doImport("examples/tests/python/example_test/hello");

    PyClass pyClass = PyClassNameIndex.find("GreetTest", myProject, false).iterator().next();
    PyClass truClass = new PyTestClass(pyClass);

    assertTrue(PyTestUtil.isPyTestClass(truClass, null));

    PsiFile file = new PyTestFile(truClass.getContainingFile(), truClass);
    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(file);
    ArrayList<String> items = new ArrayList<String>(Arrays.asList(esc.getSettings().getScriptParameters().split(" ")));

    assertNotContainsSubstring(items, "--test-junit-test");
  }

  @NotNull
  private ExternalSystemRunConfiguration getExternalSystemRunConfiguration(PsiElement psiElement) {
    ConfigurationContext context = createContext(psiElement, new MapDataContext());
    ConfigurationFromContext myPantsConfigurationFromContext = getPantsConfigurationFromContext(context);
    return (ExternalSystemRunConfiguration)myPantsConfigurationFromContext.getConfiguration();
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

  //  The classes below are created to correctly form a context for python testing
  private class PyTestFile extends PyFileImpl {

    private PyClass myTestClass;
    private PyTestFile(PsiFile pyFile, PyClass testClass) {
      super(pyFile.getViewProvider());
      myTestClass = testClass;
    }

    @Override
    public List<PyClass> getTopLevelClasses() {
      return Collections.singletonList(myTestClass);
    }
  }
  private class PyTestClass extends PyClassImpl {
    PyClass within;
    private PyTestClass(PyClass pyClass) {
      super(pyClass.getNameNode());
      within = pyClass;
    }

    @NotNull
    public List<PyClassLikeType> getAncestorTypes(@NotNull TypeEvalContext context) {
      ArrayList<PyClassLikeType> ancestors = new ArrayList<PyClassLikeType>();
      ancestors.add(new AncestorPyClass(within, true));
      return ancestors;
    }
  }

  private class AncestorPyClass extends PyClassTypeImpl {

    private AncestorPyClass(@NotNull PyClass source, boolean isDefinition) {
      super(source, isDefinition);
    }

    @Override
    public String getClassQName() {
      return "unittest.TestCase";
    }
  }
}
