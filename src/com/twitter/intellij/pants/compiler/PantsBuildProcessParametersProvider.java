// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler;

import com.intellij.compiler.server.BuildProcessParametersProvider;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.fest.util.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PantsBuildProcessParametersProvider extends BuildProcessParametersProvider {

  private final Project myProject;

  public PantsBuildProcessParametersProvider(Project project) {
    myProject = project;
  }


  @NotNull
  @Override
  public List<String> getClassPath() {
    if (PantsUtil.isPantsProject(myProject)) {
      throw new RuntimeException(PantsConstants.EXTERNAL_BUILDER_ERROR);
    }
    return Lists.newArrayList();
  }
}
