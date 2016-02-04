// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.resolver;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.metadata.TargetMetadata;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class PantsCreateModulesExtension implements PantsResolverExtension {
  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules
  ) {
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getSortedTargets()) {
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

  @NotNull
  private DataNode<ModuleData> createModuleData(
    @NotNull DataNode<ProjectData> projectInfoDataNode,
    @NotNull String targetName,
    @NotNull TargetInfo targetInfo,
    @NotNull PantsCompileOptionsExecutor executor
  ) {
    final Collection<SourceRoot> roots = targetInfo.getRoots();
    final PantsSourceType rootType = targetInfo.getSourcesType();
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
