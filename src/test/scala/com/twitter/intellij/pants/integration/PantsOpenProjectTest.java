// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public final class PantsOpenProjectTest extends OSSPantsIntegrationTest {
  public void testOpenDirectory() {
    Path path = getPath("examples/src/scala/org/pantsbuild/example/hello/");
    Project project = ProjectUtil.openOrImport(path, new OpenProjectTask());

    assertNotNull(project);
    Path rootPath = rootPath(path);
    assertEquals(rootPath, Paths.get(project.getBasePath()));

    assertOpened(project);
  }

  public void testOpenHello() {
    Path importPath = getPath("examples/src/scala/org/pantsbuild/example/hello/BUILD");
    Project project = ProjectUtil.openOrImport(importPath, new OpenProjectTask());

    assertNotNull(project);
    Path rootPath = rootPath(importPath);
    assertEquals(rootPath, Paths.get(project.getBasePath()));

    assertOpened(project);
  }

  @NotNull
  private Path rootPath(Path path) {
    return PantsUtil.findBuildRoot(path.toString()).map(VirtualFile::getPath).map(Paths::get).get();
  }

  private Path getPath(String relative) {
    return Paths.get(super.getProjectPath(), StringUtil.notNullize(relative));
  }

  private void assertOpened(Project project) {
    assertContain(Arrays.asList(ProjectUtil.getOpenProjects()), project);
  }
}
