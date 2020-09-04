// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.macro;

import com.intellij.ide.macro.Macro;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.bsp.JarMappings;
import com.twitter.intellij.pants.bsp.PantsBspData;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public class FilePathRelativeToBuiltRootMacro extends Macro {
  /**
   * @return corresponding name of this macro
   */
  @NotNull
  @Override
  public String getName() {
    return "PantsFilePathRelativeToBuildRoot";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Relative path from build root";
  }

  private Optional<String> jarFilePath(Project project, VirtualFile vFile) {
    Optional<VirtualFile> jarFile = JarMappings.getParentJar(vFile);
    if(jarFile.isPresent()) {
      JarMappings mappings = JarMappings.getInstance(project);
      Optional<Path> target = mappings
        .findTargetForJar(jarFile.get())
        .flatMap(PantsTargetAddress::tryParse)
        .map(PantsTargetAddress::getPath);
      if (target.isPresent()){
        String relativePath = target.get().resolve(vFile.getName()).toString();
        Set<VirtualFile> pantsRoots = PantsBspData.pantsRoots(project);
        boolean pathExists = pantsRoots.stream()
          .anyMatch(x -> x.findFileByRelativePath(relativePath) != null);
        if (pathExists) {
          return Optional.of(relativePath);
        } else {
          return Optional.empty();
        }
      }
    }
    return Optional.empty();
  }


  @Override
  public String expand(@NotNull final DataContext dataContext) {
    VirtualFile fileSelected = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
    if (fileSelected == null) {
      return null;
    }

    Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(fileSelected);
    if (buildRoot.isPresent()) {
      return FileUtil.getRelativePath(VfsUtil.virtualToIoFile(buildRoot.get()), VfsUtil.virtualToIoFile(fileSelected));
    }

    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if(project != null) {
      Optional<String> jarFile = jarFilePath(project, fileSelected);
      if(jarFile.isPresent()){
        return jarFile.get();
      }
    }

    return "";
  }
}
