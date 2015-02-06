// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.util.io.FileUtil;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import org.jetbrains.annotations.NotNull;

public class OSSProjectInfoParserTest extends OSSPantsIntegrationTest {
  @NotNull
  public ProjectInfo resolveProjectInfo(@NotNull String relativeProjectPath) {
    final String absoluteProjectPath = FileUtil.join(myProjectRoot.getPath(), relativeProjectPath);
    final PantsResolver resolver = new PantsResolver(absoluteProjectPath, new PantsExecutionSettings(), true);
    resolver.resolve(null);
    final ProjectInfo projectInfo = resolver.getProjectInfo();
    assertNotNull(projectInfo);
    return projectInfo;
  }

  public void testTargetType() {
    final ProjectInfo info = resolveProjectInfo("examples/src/scala/com/pants/example/hello/");

    final TargetInfo welcomeTarget = info.getTarget("examples/src/scala/com/pants/example/hello/welcome:welcome");
    assertNotNull(welcomeTarget);
    assertTrue(welcomeTarget.isScalaTarget());

    final TargetInfo greetTarget = info.getTarget("examples/src/java/com/pants/examples/hello/greet:greet");
    assertNotNull(greetTarget);
    assertFalse(greetTarget.isScalaTarget());
  }
}
