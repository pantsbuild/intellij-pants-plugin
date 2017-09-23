// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.MapDataContext;
import com.intellij.util.ThreeState;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.impl.PyClassImpl;
import com.jetbrains.python.psi.impl.PyFileImpl;
import com.jetbrains.python.psi.stubs.PyClassNameIndex;
import com.jetbrains.python.psi.types.PyClassLikeType;
import com.jetbrains.python.psi.types.PyClassTypeImpl;
import com.jetbrains.python.psi.types.TypeEvalContext;
import com.jetbrains.python.testing.PythonUnitTestUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class OSSPantsPythonRunConfigurationIntegrationTest extends OSSPantsIntegrationTest {

  public void testPyTestRunConfiguration() throws Throwable {
    doImport("examples/tests/python/example_test/hello");

    PyClass pyClass = PyClassNameIndex.find("GreetTest", myProject, true).iterator().next();
    PyClass truClass = new PyTestClass(pyClass);
    assertFalse(PythonUnitTestUtil.isTestClass(pyClass, ThreeState.YES, null));

    PsiFile file = new PyTestFile(truClass.getContainingFile(), truClass);
    ExternalSystemRunConfiguration esc = getExternalSystemRunConfiguration(file);
    List<String> items = PantsUtil.parseCmdParameters(Optional.ofNullable(esc.getSettings().getScriptParameters()));

    assertNotContainsSubstring(items, PantsConstants.PANTS_CLI_OPTION_JUNIT_TEST);
    assertContainsSubstring(items, PantsConstants.PANTS_CLI_OPTION_PYTEST);
  }

  @NotNull
  private ExternalSystemRunConfiguration getExternalSystemRunConfiguration(PsiElement psiElement) {
    ConfigurationContext context = createContext(psiElement, new MapDataContext());
    ConfigurationFromContext myPantsConfigurationFromContext = getPantsPytestConfigurationFromContext(context);
    assertNotNull(myPantsConfigurationFromContext);
    return (ExternalSystemRunConfiguration) myPantsConfigurationFromContext.getConfiguration();
  }

  private ConfigurationFromContext getPantsPytestConfigurationFromContext(ConfigurationContext context) {
    for (RunConfigurationProducer producer : RunConfigurationProducer.getProducers(myProject)) {
      if (producer instanceof PantsPythonTestRunConfigurationProducer) {
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
  private static class PyTestFile extends PyFileImpl {

    private PyClass myTestClass;

    private PyTestFile(PsiFile pyFile, PyClass testClass) {
      super(pyFile.getViewProvider());
      myTestClass = testClass;
    }

    @Override
    @NotNull
    public List<PyClass> getTopLevelClasses() {
      return Collections.singletonList(myTestClass);
    }
  }

  private static class PyTestClass extends PyClassImpl {
    PyClass within;

    private PyTestClass(PyClass pyClass) {
      super(pyClass.getNameNode());
      within = pyClass;
    }

    @NotNull
    public List<PyClassLikeType> getAncestorTypes(@NotNull TypeEvalContext context) {
      ArrayList<PyClassLikeType> ancestors = new ArrayList<PyClassLikeType>();
      ancestors.add(new AncestorPyClass(within));
      return ancestors;
    }
  }

  private static class AncestorPyClass extends PyClassTypeImpl {

    private AncestorPyClass(@NotNull PyClass source) {
      super(source, true);
    }

    @Override
    public String getClassQName() {
      return "unittest.TestCase";
    }
  }
}
