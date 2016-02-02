// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.resolver;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.metadata.TargetMetadata;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.twitter.intellij.pants.util.PantsUtil.findChildren;

public class PantsCompilerOutputsExtension implements PantsResolverExtension {
  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules
  ) {
    if (executor.isCompileWithIntellij()) {
      return;
    }
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getSortedTargets()) {
      final String targetAddress = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (modules.containsKey(targetAddress)) {
        addPantsJpsCompileOutputs(targetInfo, modules.get(targetAddress), executor);
      }
    }
  }

  private void addPantsJpsCompileOutputs(
    @NotNull TargetInfo targetInfo,
    @NotNull DataNode<ModuleData> moduleDataNode,
    @NotNull final PantsCompileOptionsExecutor executor
  ) {
    if (PantsUtil.isResource(targetInfo.getSourcesType()) || targetInfo.isDummy()) {
      return;
    }
    final List<String> compilerOutputRelativePaths = executor.isIsolatedStrategy() ?
                                                     getIsolatedCompilerOutputPath(targetInfo, executor) :
                                                     getCompilerOutputPath(targetInfo, executor);
    final List<String> compilerOutputAbsolutePaths = ContainerUtil.map(
      compilerOutputRelativePaths,
      new Function<String, String>() {
        @Override
        public String fun(String relativePath) {
          return executor.getAbsolutePathFromWorkingDir(relativePath);
        }
      }
    );
    for (TargetMetadata targetMetadata : findChildren(moduleDataNode, TargetMetadata.KEY)) {
      targetMetadata.setCompilerOutputs(new HashSet<String>(compilerOutputAbsolutePaths));
    }
  }

  @NotNull
  private List<String> getIsolatedCompilerOutputPath(@NotNull final TargetInfo targetInfo, @NotNull final PantsCompileOptionsExecutor executor) {
    return ContainerUtil.map(
      targetInfo.getAddressInfos(),
      new Function<TargetAddressInfo, String>() {
        @Override
        public String fun(TargetAddressInfo targetAddressInfo) {
          final String targetId = targetAddressInfo.getCanonicalId();
          return ".pants.d/compile/jvm/" + executor.compilerFolderForTarget(targetAddressInfo) + "/isolated-classes/" + targetId;
        }
      }
    );
  }

  @NotNull
  private List<String> getCompilerOutputPath(@NotNull TargetInfo targetInfo, @NotNull final PantsCompileOptionsExecutor executor) {
    return ContainerUtil.map(
      targetInfo.getAddressInfos(),
      new Function<TargetAddressInfo, String>() {
        @Override
        public String fun(TargetAddressInfo targetAddressInfo) {
          return ".pants.d/compile/jvm/" + executor.compilerFolderForTarget(targetAddressInfo) + "/classes";
        }
      }
    );
  }
}
