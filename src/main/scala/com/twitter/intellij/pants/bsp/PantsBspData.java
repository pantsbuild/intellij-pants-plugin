// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.bsp.BSP;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

final public class PantsBspData {
  final private Path myBspPath;
  final private VirtualFile myPantsRoot;

  public PantsBspData(Path bspPath, VirtualFile pantsRoot) {
    myBspPath = bspPath;
    myPantsRoot = pantsRoot;
  }

  public Path getBspPath() {
    return myBspPath;
  }

  public VirtualFile getPantsRoot() {
    return myPantsRoot;
  }

  public static Optional<VirtualFile> pantsRoots(Project project) {
    return Arrays.stream(ModuleManager.getInstance(project).getModules())
      .filter(module ->
                Objects
                  .equals(
                    ExternalSystemModulePropertyManager.getInstance(module).getExternalSystemId(),
                    BSP.ProjectSystemId().getId())
      )
      .map(module -> FastpassUtils.pantsRoots(module).findFirst())
      .filter(Optional::isPresent)
      .findFirst()
      .flatMap(Function.identity());
  }

  public static Optional<Path> bspRoot(Project project) {
    if(project.getBasePath() != null) {
      Path path = Paths.get(project.getBasePath());
      if(path.resolve(".bloop").toFile().exists()) {
        return Optional.of(path);
      }
    }
    return Optional.empty();
  }

  public static Optional<PantsBspData> importsFor(Project project) {
    Optional<Path> bspRoot = PantsBspData.bspRoot(project);
    Optional<VirtualFile> pantsRoot = PantsBspData.pantsRoots(project);
    if (bspRoot.isPresent() && pantsRoot.isPresent()) {
      return Optional.of(new PantsBspData(bspRoot.get(), pantsRoot.get()));
    }
    return Optional.empty();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PantsBspData data = (PantsBspData) o;
    return Objects.equals(myBspPath, data.myBspPath) &&
           Objects.equals(myPantsRoot, data.myPantsRoot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myBspPath, myPantsRoot);
  }
}
