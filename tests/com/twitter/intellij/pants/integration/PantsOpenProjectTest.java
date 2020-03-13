// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public final class PantsOpenProjectTest extends OSSPantsIntegrationTest {
  public void testOpenDirectory() {
    Path path = getPath("examples/src/scala/org/pantsbuild/example/hello/");
    Project project = ProjectUtil.openOrImport(path, new OpenProjectTask());

    assertNotNull(project);
    assertEquals(path, Paths.get(project.getBasePath()));

    assertOpened(project);
  }

  public void testOpenHello() {
    Path path = getPath("examples/src/scala/org/pantsbuild/example/hello/BUILD");
    Project project = ProjectUtil.openOrImport(path, new OpenProjectTask());

    assertNotNull(project);
    assertEquals(path.getParent(), Paths.get(project.getBasePath()));

    assertOpened(project);
  }

  private Path getPath(String relative) {
    return Paths.get(super.getProjectPath(), StringUtil.notNullize(relative));
  }

  private void assertOpened(Project project) {
    assertContain(Arrays.asList(ProjectUtil.getOpenProjects()), project);
  }
}
