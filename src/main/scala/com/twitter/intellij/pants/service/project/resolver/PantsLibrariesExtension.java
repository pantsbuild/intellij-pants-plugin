// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.resolver;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryLevel;
import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.util.io.FileUtil;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.model.LibraryInfo;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.service.project.model.graph.BuildGraph;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Optional;

public class PantsLibrariesExtension implements PantsResolverExtension {
  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules,
    @NotNull Optional<BuildGraph> buildGraph
  ) {
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getSortedTargets()) {
      final TargetInfo targetInfo = entry.getValue();

      if (executor.getOptions().isImportSourceDepsAsJars()) {
        if (targetInfo.isPythonTarget()) {
          continue;
        }
      }
      else if (!targetInfo.isJarLibrary()) {
        continue;
      }

      final String jarTarget = entry.getKey();
      final LibraryData libraryData = new LibraryData(PantsConstants.SYSTEM_ID, jarTarget);

      for (String libraryId : targetInfo.getLibraries()) {
        final LibraryInfo libraryInfo = projectInfo.getLibraries(libraryId);
        if (libraryInfo == null) {
          LOG.debug("Couldn't find library " + libraryId);
          continue;
        }

        addPathLoLibrary(libraryData, executor, LibraryPathType.BINARY, libraryInfo.getDefault());
        addPathLoLibrary(libraryData, executor, LibraryPathType.SOURCE, libraryInfo.getSources());
        addPathLoLibrary(libraryData, executor, LibraryPathType.DOC, libraryInfo.getJavadoc());

        for (String otherLibraryInfo : libraryInfo.getJarsWithCustomClassifiers()) {
          addPathLoLibrary(libraryData, executor, LibraryPathType.BINARY, otherLibraryInfo);
        }
      }

      projectDataNode.createChild(ProjectKeys.LIBRARY, libraryData);
      final DataNode<ModuleData> moduleDataNode = modules.get(jarTarget);
      if (moduleDataNode == null) {
        continue;
      }

      final LibraryDependencyData library = new LibraryDependencyData(
        moduleDataNode.getData(),
        libraryData,
        LibraryLevel.PROJECT
      );
      library.setExported(true);
      moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, library);
    }
  }

  private void addPathLoLibrary(
    @NotNull LibraryData libraryData,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull LibraryPathType binary,
    @Nullable String path
  ) {
    if (path == null) {
      return;
    }
    path = FileUtil.isAbsolute(path) ? path : executor.getAbsolutePathFromWorkingDir(path);

    if (new File(path).exists()) {
      libraryData.addPath(binary, path);
    }
  }
}
