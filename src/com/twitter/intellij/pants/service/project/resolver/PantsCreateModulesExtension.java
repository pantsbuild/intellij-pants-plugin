// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.resolver;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.BuildGraph;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.metadata.TargetMetadata;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
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
      getDepthFromUser(maxDepth);
      if (depthToInclude == null) {
        throw new PantsException("Task cancelled");
      }
      logger.info(String.format("TargetInfo level %s", depthToInclude));
      targetInfoWithinLevel =
        buildGraph.get().getNodesByLevel(depthToInclude).stream().map(BuildGraph.Node::getTargetInfo).collect(Collectors.toSet());
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
      if (targetInfo.isJarLibrary()) {
        LOG.debug("Skipping " + targetName + " because it is a jar");
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

  private void getDepthFromUser(final int maxDepth) {
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        String result = Messages.showInputDialog(
          String.format(
            "Enter the level of transitive dependencies to import min: 0, max: %s.\n" +
            "0: root level.\n" +
            "1: up to direct dependency.\n" +
            "%s: entire build graph", maxDepth, maxDepth
          ),
          "Incremental Import",
          PantsIcons.Icon, //icon
          String.valueOf(maxDepth),  //initial number
          null //validator per keystroke, not necessary in this case.
        );
        depthToInclude = result == null ? null : Integer.valueOf(result);
        if (depthToInclude == null || depthToInclude < 0 || depthToInclude > maxDepth) {
          throw new PantsException("Invalid input");
        }
      }
    }, ModalityState.NON_MODAL);
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
      new File(executor.getWorkingDir(), targetName).getAbsolutePath()
    );

    final DataNode<ModuleData> moduleDataNode = projectInfoDataNode.createChild(ProjectKeys.MODULE, moduleData);

    final TargetMetadata metadata = new TargetMetadata(PantsConstants.SYSTEM_ID, moduleName);
    metadata.setTargetAddresses(
      ContainerUtil.map(
        targetInfo.getAddressInfos(),
        new Function<TargetAddressInfo, String>() {
          @Override
          public String fun(TargetAddressInfo info) {
            return info.getTargetAddress();
          }
        }
      )
    );
    metadata.setTargetAddressInfoSet(targetInfo.getAddressInfos());
    metadata.setLibraryExcludes(targetInfo.getExcludes());
    moduleDataNode.createChild(TargetMetadata.KEY, metadata);

    return moduleDataNode;
  }
}
