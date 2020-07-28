package com.twitter.intellij.pants.testFramework;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.facet.PythonFacetSettings;
import com.jetbrains.python.module.PyModuleService;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Predicate;

abstract public class OSSPantsIntegrationTestWithPython extends OSSPantsIntegrationTest {

  @Override
  public void tearDown() throws Exception {
    Predicate<Sdk> predicate = jdk -> jdk.getName().startsWith("python");
    removeJdks(predicate);
    unsetPythonSdks(predicate);
    super.tearDown();
  }

  private void unsetPythonSdks(@NotNull final Predicate<Sdk> pred) {
    Arrays.stream(ProjectManager.getInstance().getOpenProjects())
      .flatMap(project -> Arrays.stream(ModuleManager.getInstance(project).getModules()))
      .filter(PyModuleService.getInstance()::isPythonModule)
      .flatMap(module -> Arrays.stream(FacetManager.getInstance(module).getAllFacets()))
      .map(Facet::getConfiguration)
      .filter(f -> f instanceof PythonFacetSettings)
      .map(f -> (PythonFacetSettings) f)
      .filter(f -> pred.test(f.getSdk()))
      .forEach(f -> f.setSdk(null));
  }

}
