// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.modifier;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsProjectInfoModifierExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PantsCyclicDependenciesModifier implements PantsProjectInfoModifierExtension {
  @Override
  public void modify(@NotNull ProjectInfo projectInfo, @NotNull PantsCompileOptionsExecutor executor, @NotNull Logger log) {
    final Set<Map.Entry<String, TargetInfo>> originalEntries =
      new HashSet<>(projectInfo.getTargets().entrySet());
    for (Map.Entry<String, TargetInfo> nameAndInfo : originalEntries) {
      final String targetName = nameAndInfo.getKey();
      final TargetInfo targetInfo = nameAndInfo.getValue();
      if (!projectInfo.getTargets().containsKey(targetName)) {
        // already removed
        continue;
      }
      for (String dependencyTargetName : targetInfo.getTargets()) {
        TargetInfo dependencyTargetInfo = projectInfo.getTarget(dependencyTargetName);
        if (dependencyTargetInfo != null && dependencyTargetInfo.dependOn(targetName)) {

          if (targetName.equals(dependencyTargetName)) {
            throw new PantsException(String.format("Self cyclic dependency found %s", targetName));
          }

          log.info(String.format("Found cyclic dependency between %s and %s", targetName, dependencyTargetName));

          final String combinedTargetName = combinedTargetsName(targetName, dependencyTargetName);
          final TargetInfo combinedInfo = targetInfo.union(dependencyTargetInfo);
          combinedInfo.removeDependency(targetName);
          combinedInfo.removeDependency(dependencyTargetName);
          projectInfo.addTarget(combinedTargetName, combinedInfo);

          projectInfo.replaceDependency(targetName, combinedTargetName);
          projectInfo.removeTarget(targetName);

          projectInfo.replaceDependency(dependencyTargetName, combinedTargetName);
          projectInfo.removeTarget(dependencyTargetName);
        }
      }
    }
  }

  @NotNull
  private String combinedTargetsName(String... targetNames) {
    assert targetNames.length > 0;
    String commonPrefix = targetNames[0];
    for (String name : targetNames) {
      commonPrefix = StringUtil.commonPrefix(commonPrefix, name);
    }
    final String finalCommonPrefix = commonPrefix;
    return
      commonPrefix +
      StringUtil.join(
        ContainerUtil.sorted(
          ContainerUtil.map(
            targetNames,
            new Function<String, String>() {
              @Override
              public String fun(String targetName) {
                return targetName.substring(finalCommonPrefix.length());
              }
            }
          )
        ),
        "_and_"
      );
  }
}
