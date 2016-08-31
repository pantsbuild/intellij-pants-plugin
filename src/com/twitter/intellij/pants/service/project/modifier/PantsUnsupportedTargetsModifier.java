// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.modifier;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsProjectInfoModifierExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PantsUnsupportedTargetsModifier implements PantsProjectInfoModifierExtension {
  private static final Condition<TargetInfo> SUPPORTED_TARGET_TYPES_CONDITION =
    new Condition<TargetInfo>() {
      @Override
      public boolean value(TargetInfo info) {
        return ContainerUtil.exists(info.getAddressInfos(), SUPPORTED_TARGET_ADDRESSES_CONDITION);
      }
    };

  private static final Condition<TargetAddressInfo> SUPPORTED_TARGET_ADDRESSES_CONDITION =
    new Condition<TargetAddressInfo>() {
      @Override
      public boolean value(TargetAddressInfo addressInfo) {
        final String internalPantsTargetType = addressInfo.getInternalPantsTargetType();
        // internalPantsTargetType is empty for old depmap version
        return StringUtil.isEmpty(internalPantsTargetType) ||
               PantsConstants.SUPPORTED_TARGET_TYPES.contains(internalPantsTargetType);
      }
    };

  @Override
  public void modify(@NotNull ProjectInfo projectInfo, @NotNull PantsCompileOptionsExecutor executor, @NotNull Logger log) {
    final Map<String, TargetInfo> unsupportedTargets =
      PantsUtil.filterByValue(projectInfo.getTargets(), Conditions.not(SUPPORTED_TARGET_TYPES_CONDITION));
    projectInfo.removeTargets(unsupportedTargets.keySet());
  }
}
