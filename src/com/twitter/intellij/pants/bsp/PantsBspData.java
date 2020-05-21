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
import java.util.Set;
import java.util.stream.Collectors;

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

  public static Set<PantsBspData> importsFor(Project project) {
    return
      Arrays.stream(ModuleManager.getInstance(project).getModules())
        .filter(module ->
                  Objects
                    .equals(ExternalSystemModulePropertyManager.getInstance(module).getExternalSystemId(), BSP.ProjectSystemId().getId()) &&
                  FastpassUtils.pantsRoots(module).findFirst().isPresent()
        )
        .map(module -> {
          String linkedProjectPath = ExternalSystemModulePropertyManager.getInstance(module).getLinkedProjectPath();
          Optional<VirtualFile> pantsRoots = FastpassUtils.pantsRoots(module).findFirst();
          if(linkedProjectPath != null && pantsRoots.isPresent()) {
            Path bspRoot = Paths.get(linkedProjectPath);
            return new PantsBspData(bspRoot, pantsRoots.get());
          } else {
            throw new RuntimeException("Invalid BSP-Pants import. LinkedProjectPath: " + linkedProjectPath + ", pantsRoot: " + pantsRoots.toString());
          }
        })
        .collect(Collectors.toSet());
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