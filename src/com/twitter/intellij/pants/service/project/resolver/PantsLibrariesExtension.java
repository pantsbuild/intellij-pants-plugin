// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.resolver;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryLevel;
import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.notification.PantsNotificationWrapper;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.model.LibraryInfo;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PantsLibrariesExtension implements PantsResolverExtension {
  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules
  ) {
    final Map<String, LibraryData> idToLibraryData = ContainerUtilRt.newHashMap();

    for (Map.Entry<String, TargetInfo> entry : projectInfo.getSortedTargets()) {
      final TargetInfo targetInfo = entry.getValue();
      if (!targetInfo.isJarLibrary()) {
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

      idToLibraryData.put(jarTarget, libraryData);
      projectDataNode.createChild(ProjectKeys.LIBRARY, libraryData);
    }

    for (Map.Entry<String, TargetInfo> entry : projectInfo.getSortedTargets()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!targetInfo.isJarLibrary()) {
        continue;
      }
      if (!modules.containsKey(mainTarget)) {
        continue;
      }
      final LibraryData libraryData = idToLibraryData.get(mainTarget);
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);

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

  @NotNull
  private Pair<String, TargetInfo> createEmptyModuleForLibrary(
    @NotNull String buildRoot,
    @NotNull List<Pair<String, TargetInfo>> targetNameAndInfos,
    @NotNull SourceRoot originalSourceRoot
  ) throws IOException {
    Path tempDir = Files.createTempDirectory(buildRoot);
    final String commonTargetAddress = tempDir + ":3rdparty_empty_module";
    final TargetInfo commonInfo = createTargetForSourceRootUnioningDeps(targetNameAndInfos, originalSourceRoot);
    return Pair.create(commonTargetAddress, commonInfo);
  }

  @NotNull
  private TargetInfo createTargetForSourceRootUnioningDeps(
    @NotNull List<Pair<String, TargetInfo>> targetNameAndInfos,
    @NotNull SourceRoot originalSourceRoot
  ) {
    final Iterator<Pair<String, TargetInfo>> iterator = targetNameAndInfos.iterator();
    TargetInfo commonInfo = iterator.next().getSecond();
    while (iterator.hasNext()) {
      commonInfo = commonInfo.union(iterator.next().getSecond());
    }
    // make sure we won't have cyclic deps
    commonInfo.getTargets().removeAll(
      ContainerUtil.map(
        targetNameAndInfos,
        new Function<Pair<String, TargetInfo>, String>() {
          @Override
          public String fun(Pair<String, TargetInfo> info) {
            return info.getFirst();
          }
        }
      )
    );

    final Set<SourceRoot> newRoots = ContainerUtil.newHashSet(originalSourceRoot);
    commonInfo.setRoots(newRoots);
    return commonInfo;
  }
}
