// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.modifier;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsProjectInfoModifierExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class PantsTargetNamesShortenerModifier implements PantsProjectInfoModifierExtension {

  private static final int MAX_MODULE_NAME_LENGTH = 200;

  @Override
  public void modify(@NotNull ProjectInfo projectInfo, @NotNull PantsCompileOptionsExecutor executor, @NotNull Logger log) {
    final List<String> longTargetNames = projectInfo.getTargets()
      .keySet()
      .stream()
      .filter(s -> s.length() > MAX_MODULE_NAME_LENGTH)
      .collect(Collectors.toList());

    for (String targetName : longTargetNames) {
      final String newTargetName = StringUtil.trimMiddle(targetName, MAX_MODULE_NAME_LENGTH);
      log.info(targetName + " is too long! Will replace with " + newTargetName);
      projectInfo.renameTarget(targetName, newTargetName);
    }
  }
}
