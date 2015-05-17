// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.modifier;

import com.intellij.openapi.diagnostic.Logger;
import com.twitter.intellij.pants.service.project.PantsProjectInfoModifierExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PantsInlineJarLibrariesModifier implements PantsProjectInfoModifierExtension {
  @Override
  public void modify(@NotNull ProjectInfo projectInfo, Logger log) {
    final HashMap<String, TargetInfo> newTargetInfos = new HashMap<String, TargetInfo>();
    for (Map.Entry<String, TargetInfo> targetInfoEntry : projectInfo.getTargets().entrySet()) {
      final String targetName = targetInfoEntry.getKey();
      final TargetInfo targetInfo = targetInfoEntry.getValue();
      if (targetInfo.isJarLibrary()) {
        continue;
      }
      newTargetInfos.put(targetName, inlineJarLibraries(targetInfo, projectInfo, log));
    }
    projectInfo.setTargets(newTargetInfos);
  }

  @NotNull
  private TargetInfo inlineJarLibraries(@NotNull TargetInfo targetInfo, @NotNull ProjectInfo projectInfo, Logger log) {
    final Set<String> allLibraries = new HashSet<String>(targetInfo.getLibraries());
    final Set<String> allTargets = new HashSet<String>();
    for (String dependencyTargetName : targetInfo.getTargets()) {
      final TargetInfo dependencyTargetInfo = projectInfo.getTarget(dependencyTargetName);
      if (dependencyTargetInfo == null) {
        log.warn(dependencyTargetName + " is missing!");
        continue;
      }
      if (dependencyTargetInfo.isJarLibrary()) {
        allLibraries.addAll(dependencyTargetInfo.getLibraries());
      } else {
        allTargets.add(dependencyTargetName);
      }
    }

    return new TargetInfo(
      targetInfo.getAddressInfos(),
      allTargets,
      allLibraries,
      targetInfo.getRoots()
    );
  }
}
