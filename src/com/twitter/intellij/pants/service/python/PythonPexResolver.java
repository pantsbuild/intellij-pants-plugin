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
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class PythonPexResolver implements PantsResolverExtension {
  private static final Logger LOG = Logger.getInstance(PythonPexResolver.class);

  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules
  ) {
    final VirtualFile workingDir = PantsUtil.findPantsWorkingDir(projectDataNode.getData().getLinkedExternalProjectPath());
    final VirtualFile bootstrappedPants = workingDir != null ? workingDir.findChild(PantsConstants.PANTS_PEX) : null;
    final VirtualFile pexFile = bootstrappedPants != null ? bootstrappedPants : findSpecificPexVersionInHomeDirectory(workingDir);
    if (pexFile != null) {
      final LibraryData libraryData = new LibraryData(PantsConstants.SYSTEM_ID, PantsConstants.PANTS_LIBRARY_NAME);
      libraryData.addPath(LibraryPathType.BINARY, pexFile.getPath());
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

  @Nullable
  private VirtualFile findSpecificPexVersionInHomeDirectory(VirtualFile workingDir) {
    final String pantsVersion = PantsUtil.findPantsVersion(workingDir);
    if (pantsVersion == null) {
      LOG.warn(PantsBundle.message("pants.library.no.version"));
      return null;
    }

    final VirtualFile folderWithPex = PantsUtil.findFolderWithPex();
    if (folderWithPex == null) {
      LOG.warn(PantsBundle.message("pants.library.no.pex.folder"));
      return null;
    }

    final VirtualFile pexFile = PantsUtil.findPexVersionFile(folderWithPex, pantsVersion);
    if (pexFile == null) {
      LOG.warn(PantsBundle.message("pants.library.no.pex.file", pantsVersion));
      return null;
    }
    return pexFile;
  }
}
