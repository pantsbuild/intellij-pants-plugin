// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;


import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FastpassConfigSpecService {
  private Project myProject;

  Set<String> myModules;
  CompletableFuture<Set<String>> myCachedTargetSpec;

  public FastpassConfigSpecService(Project project) {
    myProject = project;
  }

  public static FastpassConfigSpecService getInstance(Project project) {
    return ServiceManager.getService(project, FastpassConfigSpecService.class);
  }

  synchronized public CompletableFuture<Set<String>> getTargetSpecs() {
    Set<String> moduleHashes =
      Arrays.stream(ModuleManager.getInstance(myProject).getModules()).map(Module::getName).collect(Collectors.toSet());
    if (!Objects.equals(moduleHashes, myModules) || myCachedTargetSpec == null) {
      myCachedTargetSpec = configSpecForPants();
      myModules = moduleHashes;
    }
    return myCachedTargetSpec;
  }

  private CompletableFuture<Set<String>> configSpecForPants() {
    Optional<PantsBspData> linkedProjects = PantsBspData.importsFor(myProject);
    return linkedProjects
      .map(FastpassUtils::selectedTargets)
      .orElseGet(() -> CompletableFuture.completedFuture(Collections.emptySet()));
  }
}
