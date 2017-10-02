// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.python;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.model.graph.BuildGraph;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class PythonPexResolver implements PantsResolverExtension {
  private static final Logger LOG = Logger.getInstance(PythonPexResolver.class);

  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules,
    @NotNull Optional<BuildGraph> buildGraph
  ) {
    final Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(projectDataNode.getData().getLinkedExternalProjectPath());
    final Optional<VirtualFile> bootstrappedPants =  buildRoot.map(file -> file.findChild(PantsConstants.PANTS_PEX));
    final Optional<VirtualFile> pexFile = bootstrappedPants.flatMap(file -> findSpecificPexVersionInHomeDirectory(buildRoot));
    if (pexFile.isPresent()) {
      final LibraryData libraryData = new LibraryData(PantsConstants.SYSTEM_ID, PantsConstants.PANTS_LIBRARY_NAME);
      libraryData.addPath(LibraryPathType.BINARY, pexFile.get().getPath());
      projectDataNode.createChild(ProjectKeys.LIBRARY, libraryData);

      for (DataNode<ModuleData> moduleDataNode : modules.values()) {
        final LibraryDependencyData library = new LibraryDependencyData(
          moduleDataNode.getData(),
          libraryData,
          LibraryLevel.PROJECT
        );
        library.setExported(false);
        moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, library);
      }
    }
  }

  private Optional<VirtualFile> findSpecificPexVersionInHomeDirectory(Optional<VirtualFile> workingDir) {
    final Optional<String> pantsVersion = PantsUtil.findPantsVersion(workingDir);
    if (!pantsVersion.isPresent()) {
      LOG.warn(PantsBundle.message("pants.library.no.version"));
      return Optional.empty();
    }

    final Optional<VirtualFile> folderWithPex = PantsUtil.findFolderWithPex();
    if (!folderWithPex.isPresent()) {
      LOG.warn(PantsBundle.message("pants.library.no.pex.folder"));
      return Optional.empty();
    }

    final Optional<VirtualFile> pexFile = folderWithPex.flatMap(file -> PantsUtil.findPexVersionFile(file, pantsVersion.get()));
    if (!pexFile.isPresent()) {
      LOG.warn(PantsBundle.message("pants.library.no.pex.file", pantsVersion));
      return Optional.empty();
    }
    return pexFile;
  }
}
