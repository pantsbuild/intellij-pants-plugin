// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.scala;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsScalaUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScalaSdkResolver implements PantsResolverExtension {
  private static final Logger LOG = Logger.getInstance(ScalaSdkResolver.class);

  @Override
  public void resolve(
    ProjectInfo projectInfo, DataNode<ProjectData> projectDataNode, Map<String, DataNode<ModuleData>> modules
  ) {
    final Set<String> scalaJars = new HashSet<String>();
    for (String libId : projectInfo.getLibraries().keySet()) {
      if (PantsScalaUtil.isScalaLib(libId)) {
        scalaJars.addAll(projectInfo.getLibraries(libId));
      }
    }

    if (scalaJars.isEmpty()) {
      return;
    }

    // add some additional
    final Set<String> addtionalScalaJars = new HashSet<String>();
    for (String scalaLibNameToAdd : PantsScalaUtil.getScalaLibNamesToAdd()) {
      for (String jarPath : scalaJars) {
        findAndAddScalaLib(addtionalScalaJars, jarPath, scalaLibNameToAdd);
      }
    }
    scalaJars.addAll(addtionalScalaJars);

    final List<String> libraryNames = ContainerUtil.mapNotNull(
      scalaJars,
      new Function<String, String>() {
        @Override
        public String fun(String path) {
          final String fileName = FileUtil.getNameWithoutExtension(PathUtil.getFileName(path));
          if (fileName.startsWith("scala-library-")) {
            return fileName;
          }
          return null;
        }
      }
    );

    String scalaLibraryName = null;
    if (libraryNames.isEmpty()) {
      LOG.warn("Couldn't find scala version in " + scalaJars);
    } else if (libraryNames.size() > 1) {
      scalaLibraryName = libraryNames.iterator().next();
      LOG.warn(String.format("There are several versions of scala in %s. %s will be used.", scalaJars, scalaLibraryName));
    } else {
      scalaLibraryName = libraryNames.iterator().next();
    }
    final LibraryData libraryData =
      new LibraryData(PantsConstants.SYSTEM_ID, StringUtil.notNullize(scalaLibraryName, "scala-library"));
    for (String jarPath : scalaJars) {
      // todo: sources + docs
      libraryData.addPath(LibraryPathType.BINARY, jarPath);
    }

    projectDataNode.createChild(ProjectKeys.LIBRARY, libraryData);
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getTargets().entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      if (moduleDataNode == null) {
        continue; // shouldn't happened because we created all modules for each target
      }
      final LibraryDependencyData library = new LibraryDependencyData(
        moduleDataNode.getData(),
        libraryData,
        LibraryLevel.PROJECT
      );
      moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, library);
      if (targetInfo.isScalaTarget()) {
        createScalaSdkDataFromJars(moduleDataNode, scalaJars);
      }
    }
  }

  private void createScalaSdkDataFromJars(@NotNull DataNode<ModuleData> moduleDataNode, Set<String> scalaLibJars) {
    final ScalaModelData scalaModelData = new ScalaModelData(PantsConstants.SYSTEM_ID);
    final Set<File> files = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
    for (String jarPath : scalaLibJars) {
      final File jarFile = new File(jarPath);
      if (jarFile.exists()) {
        files.add(jarFile);
      }
    }
    if (!files.isEmpty()) {
      // todo(fkorotkov): provide Scala info from the goal
      scalaModelData.setScalaCompilerJars(files);
      moduleDataNode.createChild(ScalaModelData.KEY, scalaModelData);
    }
  }

  private void findAndAddScalaLib(Set<String> files, String jarPath, String libName) {
    final File compilerFile = new File(StringUtil.replace(jarPath, "scala-library", libName));
    if (compilerFile.exists()) {
      files.add(compilerFile.getAbsolutePath());
    }
    else {
      LOG.warn("Could not find scala library path" + compilerFile);
    }
  }
}
