// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.modifier;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsProjectInfoModifierExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

public class PantsUnsupportedTargetsModifier implements PantsProjectInfoModifierExtension {

  @Override
  public void modify(@NotNull ProjectInfo projectInfo, @NotNull PantsCompileOptionsExecutor executor, @NotNull Logger log) {
    final Map<String, TargetInfo> unsupportedTargets = projectInfo.getTargets().entrySet().stream()
      .filter(entry -> entry.getValue().getAddressInfos().stream()
        .anyMatch(targetAddressInfo -> {
          final String type = targetAddressInfo.getInternalPantsTargetType();
          return !StringUtil.isEmpty(type) &&
                 !PantsConstants.SUPPORTED_TARGET_TYPES.contains(type);
        }))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    projectInfo.removeTargets(unsupportedTargets.keySet());
  }
}
