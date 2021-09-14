// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.modifier;

import com.intellij.openapi.diagnostic.Logger;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsProjectInfoModifierExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PantsEmptyTargetRemover implements PantsProjectInfoModifierExtension {
  @Override
  public void modify(@NotNull ProjectInfo projectInfo, @NotNull PantsCompileOptionsExecutor executor, @NotNull Logger log) {
    final List<String> emptyTargets = new ArrayList<>();
    do {
      emptyTargets.clear();
      for (Map.Entry<String, TargetInfo> targetInfoEntry : projectInfo.getTargets().entrySet()) {
        final String targetName = targetInfoEntry.getKey();
        final TargetInfo targetInfo = targetInfoEntry.getValue();
        if (targetInfo.isEmpty()) {
          emptyTargets.add(targetName);
        }
      }
      projectInfo.removeTargets(emptyTargets);
    } while (!emptyTargets.isEmpty());
  }
}
