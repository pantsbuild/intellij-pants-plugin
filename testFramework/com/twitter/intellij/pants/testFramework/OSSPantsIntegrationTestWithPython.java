package com.twitter.intellij.pants.testFramework;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.facet.PythonFacetSettings;
import com.jetbrains.python.module.PyModuleService;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.PythonSetup;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
      .flatMap(module -> Arrays.stream(FacetManager.getInstance(module).getAllFacets()))
      .map(Facet::getConfiguration)
      .filter(f -> f instanceof PythonFacetSettings)
      .map(f -> (PythonFacetSettings) f)
      .filter(f -> pred.test(f.getSdk()))
      .forEach(f -> f.setSdk(null));
  }

  public List<String> pythonRoots(List<String> targets) {
    final boolean libsWithSourcesAndDocs = true;
    final boolean useIdeaProjectJdk = false;
    final boolean isExportDepAsJar = false;
    final Optional<Integer> incrementalImportDepth = Optional.empty();
    final boolean isUseIntelliJCompiler = false;

    PantsExecutionSettings settings = new PantsExecutionSettings(
            targets,
            libsWithSourcesAndDocs,
            useIdeaProjectJdk,
            isExportDepAsJar,
            incrementalImportDepth,
            isUseIntelliJCompiler
    );

    final PantsResolver resolver =
            new PantsResolver(PantsCompileOptionsExecutor.create(myProjectRoot.getPath(), settings));
    resolver.resolve(s -> {}, null);
    final ProjectInfo projectInfo = resolver.getProjectInfo();
    assertNotNull(projectInfo);
    PythonSetup pythonSetup = projectInfo.getPythonSetup();
    assertNotNull(pythonSetup);
    return projectInfo.getPythonSetup().getInterpreters().values()
            .stream().map(i -> Paths.get(i.getBinary()).getParent().getParent().toString()).collect(Collectors.toList());
  }
}
