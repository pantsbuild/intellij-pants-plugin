// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.model.graph.BuildGraph;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface PantsResolverExtension {
  Logger LOG = Logger.getInstance(PantsResolverExtension.class);
  ExtensionPointName<PantsResolverExtension> EP_NAME = ExtensionPointName.create("com.intellij.plugins.pants.projectResolver");

  void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules,
    @NotNull Optional<BuildGraph> buildGraph
  );
}
