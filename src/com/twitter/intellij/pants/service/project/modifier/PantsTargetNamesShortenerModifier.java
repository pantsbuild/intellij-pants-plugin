// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
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
import com.twitter.intellij.pants.service.project.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class PantsTargetNamesShortenerModifier implements PantsProjectInfoModifierExtension {

  @Override
  public void modify(@NotNull ProjectInfo projectInfo, @NotNull PantsCompileOptionsExecutor executor, @NotNull Logger log) {
    final List<String> longTargetNames = ContainerUtil.findAll(
      projectInfo.getTargets().keySet(),
      new Condition<String>() {
        @Override
        public boolean value(String targetName) {
          return targetName.length() > 200;
        }
      }
    );
    for (String targetName : longTargetNames) {
      final String newTargetName = StringUtil.trimMiddle(targetName, 200);
      log.info(targetName + " is too long! Will replace with " + newTargetName);
      projectInfo.renameTarget(targetName, newTargetName);
    }
  }
}
