// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.resolver;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ModuleSdkData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.ProjectSdkData;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.metadata.TargetMetadata;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.service.project.model.graph.BuildGraph;
import com.twitter.intellij.pants.service.project.model.graph.BuildGraphNode;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PantsCreateModulesExtension implements PantsResolverExtension {

  private static Logger logger = Logger.getInstance("#" + PantsCreateModulesExtension.class.getName());

  private Integer depthToInclude = null;

  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules,
    @NotNull Optional<BuildGraph> buildGraph
  ) {
    Set<TargetInfo> targetInfoWithinLevel = null;
    if (buildGraph.isPresent()) {
      final int maxDepth = buildGraph.get().getMaxDepth();
      depthToInclude = executor.getIncrementalImportDepth().orElse(null);
      if (depthToInclude == null) {
        throw new PantsException("Task cancelled");
      }
      logger.info(String.format("TargetInfo level %s", depthToInclude));
      targetInfoWithinLevel = buildGraph
        .get()
        .getNodesUpToLevel(depthToInclude)
        .stream()
        .map(BuildGraphNode::getTargetInfo)
        .collect(Collectors.toSet());
    }

    for (Map.Entry<String, TargetInfo> entry : projectInfo.getSortedTargets()) {
      if (targetInfoWithinLevel != null && !targetInfoWithinLevel.contains(entry.getValue())) {
        continue;
      }
      final String targetName = entry.getKey();
      if (StringUtil.startsWith(targetName, ":scala-library")) {
        // we already have it in libs
        continue;
      }
      final TargetInfo targetInfo = entry.getValue();
      if (targetInfo.isEmpty()) {
        LOG.debug("Skipping " + targetName + " because it is empty");
        continue;
      }
      final DataNode<ModuleData> moduleData =
        createModuleData(
          projectDataNode,
          targetName,
          targetInfo,
          executor
        );
      modules.put(targetName, moduleData);
    }
  }

  @NotNull
  private DataNode<ModuleData> createModuleData(
    @NotNull DataNode<ProjectData> projectInfoDataNode,
    @NotNull String targetName,
    @NotNull TargetInfo targetInfo,
    @NotNull PantsCompileOptionsExecutor executor
  ) {
    final String moduleName = PantsUtil.getCanonicalModuleName(targetName);

    final ModuleData moduleData = new ModuleData(
      targetName,
      PantsConstants.SYSTEM_ID,
      ModuleTypeId.JAVA_MODULE,
      moduleName,
      projectInfoDataNode.getData().getIdeProjectFileDirectoryPath() + "/" + moduleName,
      new File(executor.getBuildRoot(), targetName).getAbsolutePath()
    );

    final DataNode<ModuleData> moduleDataNode = projectInfoDataNode.createChild(ProjectKeys.MODULE, moduleData);

    if(targetInfo.isPythonTarget()) {
      // FIXME this happens to work because a SDK with the matching name is created later. See PantsPythonSetupDataService.java:84
      String sdkName = String.format("Python for %s", projectInfoDataNode.getData().getExternalName());
      moduleDataNode.createChild(ModuleSdkData.KEY, new ModuleSdkData(sdkName));
    } else {
      DataNode<ProjectSdkData> sdk = ExternalSystemApiUtil.find(projectInfoDataNode, ProjectSdkData.KEY);
      if(sdk != null){
        ModuleSdkData moduleSdk = new ModuleSdkData(sdk.getData().getSdkName());
        moduleDataNode.createChild(ModuleSdkData.KEY, moduleSdk);
      }
    }

    final TargetMetadata metadata = new TargetMetadata(PantsConstants.SYSTEM_ID, moduleName);
    metadata.setTargetAddresses(ContainerUtil.map(targetInfo.getAddressInfos(), TargetAddressInfo::getTargetAddress));
    metadata.setTargetAddressInfoSet(targetInfo.getAddressInfos());
    metadata.setLibraryExcludes(targetInfo.getExcludes());
    moduleDataNode.createChild(TargetMetadata.KEY, metadata);

    return moduleDataNode;
  }
}
