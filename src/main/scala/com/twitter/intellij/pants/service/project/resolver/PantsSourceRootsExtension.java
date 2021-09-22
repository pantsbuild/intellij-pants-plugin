// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.resolver;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.model.ContentRoot;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.service.project.model.graph.BuildGraph;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PantsSourceRootsExtension implements PantsResolverExtension {

  private static String getSourceRootRegardingTargetType(@NotNull TargetInfo targetInfo, @NotNull ContentRoot root) {
    return doNotSupportPackagePrefixes(targetInfo) ? root.getPackageRoot() : root.getRawSourceRoot();
  }

  private static boolean doNotSupportPackagePrefixes(@NotNull TargetInfo targetInfo) {
    return targetInfo.isPythonTarget() || PantsSourceType.isResource(targetInfo.getSourcesType());
  }

  private static boolean hasAnAncestorRoot(@NotNull Collection<String> baseSourceRoots, @NotNull String root) {
    for (String sourceRoot : baseSourceRoots) {
      if (FileUtil.isAncestor(sourceRoot, root, false)) {
        return true;
      }
    }
    return false;
  }


  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules,
    @NotNull Optional<BuildGraph> buildGraph
  ) {
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getSortedTargets()) {
      final String targetAddress = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(targetAddress)) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(targetAddress);

      createContentRoots(moduleDataNode, targetInfo);
    }
  }

  private void createContentRoots(@NotNull DataNode<ModuleData> moduleDataNode, @NotNull final TargetInfo targetInfo) {
    final Set<ContentRoot> roots = targetInfo.getRoots();
    if (roots.isEmpty()) {
      return;
    }

    for (String baseRoot : findBaseRoots(targetInfo, roots)) {
      final ContentRootData contentRoot = new ContentRootData(PantsConstants.SYSTEM_ID, baseRoot);
      moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);

      for (ContentRoot sourceRoot : roots) {
        final String sourceRootPathToAdd = getSourceRootRegardingTargetType(targetInfo, sourceRoot);
        if (FileUtil.isAncestor(baseRoot, sourceRootPathToAdd, false)) {
          try {
            contentRoot.storePath(
              targetInfo.getSourcesType().toExternalSystemSourceType(),
              sourceRootPathToAdd,
              doNotSupportPackagePrefixes(targetInfo) ? null : sourceRoot.getPackagePrefix()
            );
          }
          catch (IllegalArgumentException e) {
            LOG.warn(e);
            // todo(fkorotkov): log and investigate exceptions from ContentRootData.storePath(ContentRootData.java:94)
          }
        }
      }
    }
  }

  @NotNull
  private List<String> findBaseRoots(@NotNull final TargetInfo targetInfo, Set<ContentRoot> roots) {
    Set<String> allRoots = roots.stream()
      .map(root -> getSourceRootRegardingTargetType(targetInfo, root))
      .collect(Collectors.toSet());

    final List<String> sortedRoots = ContainerUtil.sorted(
      allRoots,
      StringUtil::naturalCompare
    );

    final List<String> baseRoots = new ArrayList<>();
    for (String root : sortedRoots) {
      if (!hasAnAncestorRoot(baseRoots, root)) {
        baseRoots.add(root);
      }
    }
    return baseRoots;
  }
}
