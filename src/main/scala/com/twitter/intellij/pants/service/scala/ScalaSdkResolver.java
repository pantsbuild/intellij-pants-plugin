// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.scala;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryLevel;
import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.model.graph.BuildGraph;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.model.LibraryInfo;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsScalaUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ScalaSdkResolver implements PantsResolverExtension {
  private static final Logger LOG = Logger.getInstance(ScalaSdkResolver.class);

  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules,
    @NotNull Optional<BuildGraph> buildGraph
  ) {
    final Map<String, Set<String>> scalaLibId2Jars = new HashMap<>();
    for (String libId : ContainerUtil.sorted(projectInfo.getLibraries().keySet())) {
      if (PantsScalaUtil.isScalaLibraryLib(libId)) {
        final LibraryInfo scalaLib = projectInfo.getLibraries(libId);
        final String scalaLibPath = scalaLib != null ? scalaLib.getDefault() : null;
        if (scalaLibPath == null) {
          continue;
        }
        final Set<String> scalaSdkJars = new HashSet<>();
        for (String scalaLibNameToAdd : PantsScalaUtil.getScalaLibNamesToAdd()) {
          findAndAddScalaLib(scalaSdkJars, scalaLibPath, scalaLibNameToAdd);
        }
        scalaLibId2Jars.put(libId, scalaSdkJars);
      }
    }

    final String defaultScalaLibId = ContainerUtil.getFirstItem(scalaLibId2Jars.keySet());

    if (defaultScalaLibId == null) {
      LOG.debug("Didn't find any Scala libraries");
      // no scala libs - no problems
      return;
    }

    final Map<String, LibraryData> scalaLibId2Data = new HashMap<>();
    for (Map.Entry<String, Set<String>> entry : scalaLibId2Jars.entrySet()) {
      final String scalaLibraryId = entry.getKey();
      final Set<String> scalaJars = entry.getValue();

      final LibraryData libraryData =
        new LibraryData(PantsConstants.SYSTEM_ID, scalaLibraryId);
      for (String jarPath : scalaJars) {
        // todo: sources + docs
        libraryData.addPath(LibraryPathType.BINARY, jarPath);
      }
      projectDataNode.createChild(ProjectKeys.LIBRARY, libraryData);
      scalaLibId2Data.put(scalaLibraryId, libraryData);
    }

    for (Map.Entry<String, TargetInfo> entry : projectInfo.getTargets().entrySet()) {
      final String mainTarget = entry.getKey();
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      final TargetInfo targetInfo = entry.getValue();
      if (moduleDataNode != null && targetInfo.isScalaTarget()) {
        String scalaLibId = StringUtil.notNullize(targetInfo.findScalaLibId(), defaultScalaLibId);
        LibraryData libraryData = scalaLibId2Data.get(scalaLibId);

        if (libraryData == null) {
          LOG.warn("Can't find Scala SDK for " + scalaLibId);
          final Map.Entry<String, LibraryData> libraryDataEntry = scalaLibId2Data.entrySet().iterator().next();
          scalaLibId = libraryDataEntry.getKey();
          libraryData = libraryDataEntry.getValue();
        }

        final LibraryDependencyData libraryDependencyData = new LibraryDependencyData(moduleDataNode.getData(), libraryData, LibraryLevel.PROJECT);
        moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData);

        final ScalaModelData scalaModelData = new ScalaModelData(scalaLibId, libraryData.getPaths(LibraryPathType.BINARY));
        moduleDataNode.createChild(ScalaModelData.KEY, scalaModelData);
      }
    }
  }

  private void findAndAddScalaLib(Set<String> files, String jarPath, String libName) {
    final File libFile = PantsScalaUtil.getScalaLibFile(jarPath, libName);
    if (libFile.exists()) {
      files.add(libFile.getAbsolutePath());
    } else {
      LOG.warn("Could not find scala library path: " + libFile);
    }
  }
}
