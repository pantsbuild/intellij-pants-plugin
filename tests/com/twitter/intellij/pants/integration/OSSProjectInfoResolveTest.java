// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.util.Consumer;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.service.project.model.LibraryInfo;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

public class OSSProjectInfoResolveTest extends OSSPantsIntegrationTest {
  private static Consumer<String> STRING_CONSUMER = new Consumer<String>() {
    public void consume(final String t) {
    }
  };

  private void assertPathContainsJar(String path, String jarName) {
    assertTrue(String.format("%s is not found in path %s", jarName, path), path.endsWith(jarName));
  }

  @NotNull
  private ProjectInfo resolveProjectInfo(@NotNull String targetSpec) {
    final boolean withDependees = false;
    final boolean libsWithSourcesAndDocs = true;
    final boolean useIdeaProjectJdk = false;
    final boolean isEnableIncrementalImport = false;
    PantsExecutionSettings settings = new PantsExecutionSettings(
      Collections.singletonList(targetSpec),
      withDependees,
      libsWithSourcesAndDocs,
      useIdeaProjectJdk,
      isEnableIncrementalImport
    );

    final PantsResolver resolver =
      new PantsResolver(PantsCompileOptionsExecutor.create(myProjectRoot.getPath(), settings));
    resolver.resolve(STRING_CONSUMER, null);
    final ProjectInfo projectInfo = resolver.getProjectInfo();
    assertNotNull(projectInfo);
    return projectInfo;
  }

  public void testTargetType() {
    final ProjectInfo info = resolveProjectInfo("examples/src/scala/org/pantsbuild/example/hello/");

    final TargetInfo welcomeTarget = info.getTarget("examples/src/scala/org/pantsbuild/example/hello/welcome:welcome");
    assertNotNull(welcomeTarget);
    assertTrue(welcomeTarget.isScalaTarget());

    final TargetInfo greetTarget = info.getTarget("examples/src/java/org/pantsbuild/example/hello/greet:greet");
    assertNotNull(greetTarget);
    assertFalse(greetTarget.isScalaTarget());
  }

  public void testTargetJars() {
    final ProjectInfo info = resolveProjectInfo("intellij-integration/3rdparty/hadoop/::");

    final TargetInfo welcomeTarget = info.getTarget("intellij-integration/3rdparty/hadoop:hadoop-stuff");
    assertNotNull(welcomeTarget);

    LibraryInfo lib = info.getLibraries("org.apache.hadoop:hadoop-common:2.7.1");
    assertNotNull(lib);

    assertPathContainsJar(lib.getDefault(), "hadoop-common-2.7.1.jar");
    assertPathContainsJar(lib.getJavadoc(), "hadoop-common-2.7.1-javadoc.jar");
    assertPathContainsJar(lib.getSources(), "hadoop-common-2.7.1-sources.jar");

    assertEquals(lib.getJarsWithCustomClassifiers().size(), 1);
    assertPathContainsJar(lib.getJarsWithCustomClassifiers().iterator().next(), "hadoop-common-2.7.1-tests.jar");
  }

  public void testListTargets() {
    assertEquals(
      Arrays.asList(
        "examples/src/java/org/pantsbuild/example/hello/main:main",
        "examples/src/java/org/pantsbuild/example/hello/main:readme",
        "examples/src/java/org/pantsbuild/example/hello/main:main-bin"
      ),
      PantsUtil.listAllTargets(myProjectRoot.getPath() + File.separator + "examples/src/java/org/pantsbuild/example/hello/main/BUILD")
    );
  }
}
