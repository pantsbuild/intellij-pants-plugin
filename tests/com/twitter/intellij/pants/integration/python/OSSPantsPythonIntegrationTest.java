// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.python;

import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTestWithPython;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OSSPantsPythonIntegrationTest extends OSSPantsIntegrationTestWithPython {

  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "PythonCore");
  }

  public List<String> pythonRoots() { // todo DRY
    final boolean libsWithSourcesAndDocs = true;
    final boolean useIdeaProjectJdk = false;
    final boolean isExportDepAsJar = false;
    final Optional<Integer> incrementalImportDepth = Optional.empty();
    final boolean isUseIntelliJCompiler = false;

    PantsExecutionSettings settings = new PantsExecutionSettings(
      Arrays.asList("src/python::", "tests/python/pants_test::", "contrib/::"),
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
    return projectInfo.getPythonSetup().getInterpreters().values()
      .stream().map(i -> Paths.get(i.getBinary()).getParent().getParent().toString()).collect(Collectors.toList());

  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    List<String> pythonRoots = pythonRoots();
    // todo: Remove if possible. Now the test fails with VfsRootAccess to python interpreter in /opt
    VfsRootAccess.allowRootAccess(myProject, pythonRoots.toArray(new String[0]));
  }

  public void testIntelliJIntegration() throws Throwable {
    final String pythonScript = "build-support/pants-intellij.sh";
    if (myProjectRoot.findFileByRelativePath(pythonScript) == null) {
      return;
    }
    doImport(pythonScript);

    assertModuleExists("python_src");
    assertModuleExists("python_tests");
    assertModuleExists("python_requirements");
  }
}
