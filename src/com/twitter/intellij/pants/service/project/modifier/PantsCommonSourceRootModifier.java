// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.modifier;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsProjectInfoModifierExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class PantsCommonSourceRootModifier implements PantsProjectInfoModifierExtension {
  public static final String COMMON_SOURCES_TARGET_NAME = "common_sources";

  @Override
  public void modify(@NotNull ProjectInfo projectInfo, @NotNull PantsCompileOptionsExecutor executor, @NotNull Logger log) {
    // IntelliJ doesn't support when several modules have the same source root
    // so, for source roots that point at multiple targets, we need to convert those so that
    // they have only one target that owns them.
    // to do that, we
    // - find or create a target to own the source root
    // - for each target that depends on that root,
    //   we replace the root with a dependency on the new target

    final Map<SourceRoot, List<Pair<String, TargetInfo>>> sourceRoot2Targets = getSourceRoot2TargetMapping(projectInfo);
    if (sourceRoot2Targets.isEmpty()) {
      return;
    }

    for (Map.Entry<SourceRoot, List<Pair<String, TargetInfo>>> entry : sourceRoot2Targets.entrySet()) {
      final List<Pair<String, TargetInfo>> targetNameAndInfos = entry.getValue();
      final SourceRoot commonSourceRoot = entry.getKey();
      if (targetNameAndInfos.size() <= 1) {
        continue;
      }

      final Pair<String, TargetInfo> commonTargetNameAndInfo =
        createTargetForCommonSourceRoot(executor.getWorkingDir().getPath(), targetNameAndInfos, commonSourceRoot);

      projectInfo.addTarget(commonTargetNameAndInfo.getFirst(), commonTargetNameAndInfo.getSecond());
      for (Pair<String, TargetInfo> nameAndInfo : targetNameAndInfos) {
        nameAndInfo.getSecond().getRoots().remove(commonSourceRoot);
        nameAndInfo.getSecond().addDependency(commonTargetNameAndInfo.getFirst());
      }
    }
  }

  @NotNull
  public Map<SourceRoot, List<Pair<String, TargetInfo>>> getSourceRoot2TargetMapping(@NotNull ProjectInfo projectInfo) {
    final Factory<List<Pair<String, TargetInfo>>> listFactory = ArrayList::new;
    final Map<SourceRoot, List<Pair<String, TargetInfo>>> result = new HashMap<>();
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getTargets().entrySet()) {
      final String targetName = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      for (SourceRoot sourceRoot : targetInfo.getRoots()) {
        ContainerUtil.getOrCreate(
          result,
          sourceRoot,
          listFactory
        ).add(Pair.create(targetName, targetInfo));
      }
    }
    return result;
  }

  @NotNull
  private Pair<String, TargetInfo> createTargetForCommonSourceRoot(
    @NotNull String path,
    @NotNull List<Pair<String, TargetInfo>> targetNameAndInfos,
    @NotNull SourceRoot originalSourceRoot
  ) {
    final String commonTargetAddress = createTargetAddressForCommonSource(path, originalSourceRoot);
    final TargetInfo commonInfo = createTargetForSourceRootUnioningDeps(targetNameAndInfos, originalSourceRoot);
    return Pair.create(commonTargetAddress, commonInfo);
  }

  @NotNull
  private String createTargetAddressForCommonSource(@NotNull String projectPath, @NotNull SourceRoot originalSourceRoot) {
    final String commonPath = originalSourceRoot.getRawSourceRoot();
    final String relativePath = commonPath.substring(projectPath.length());
    return relativePath + ":" + COMMON_SOURCES_TARGET_NAME;
  }

  @NotNull
  private TargetInfo createTargetForSourceRootUnioningDeps(
    @NotNull List<Pair<String, TargetInfo>> targetNameAndInfos,
    @NotNull SourceRoot originalSourceRoot
  ) {
    final Iterator<Pair<String, TargetInfo>> iterator = targetNameAndInfos.iterator();
    TargetInfo commonInfo = iterator.next().getSecond();
    while (iterator.hasNext()) {
      commonInfo = commonInfo.union(iterator.next().getSecond());
    }
    // make sure we won't have cyclic deps
    commonInfo.getTargets().removeAll(
      ContainerUtil.map(
        targetNameAndInfos,
        new Function<Pair<String, TargetInfo>, String>() {
          @Override
          public String fun(Pair<String, TargetInfo> info) {
            return info.getFirst();
          }
        }
      )
    );

    final Set<SourceRoot> newRoots = ContainerUtil.newHashSet(originalSourceRoot);
    commonInfo.setRoots(newRoots);
    return commonInfo;
  }
}
