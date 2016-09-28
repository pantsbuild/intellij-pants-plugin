// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.python;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.io.FileUtil;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.model.graph.BuildGraph;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.PythonSetup;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 *  Currently we configure requirements only for default interpreter.
 *
 *  @see PythonInfoModifier for details.
 */
public class PythonRequirementsResolver implements PantsResolverExtension {
  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules,
    @NotNull Optional<BuildGraph> buildGraph
  ) {
    final PythonSetup pythonSetup = projectInfo.getPythonSetup();
    if (pythonSetup == null) {
      LOG.warn("Current version of Pants doesn't provide information about Python setup. Please upgrade!");
      return;
    }

    final DataNode<ModuleData> requirementsModuleDataNode = createRequirementsModule(projectDataNode, pythonSetup, executor);
    requirementsModuleDataNode.createChild(
      PythonSetupData.KEY,
      new PythonSetupData(requirementsModuleDataNode.getData(), pythonSetup.getDefaultInterpreterInfo())
    );

    for (Map.Entry<String, TargetInfo> targetInfoEntry : projectInfo.getSortedTargets()) {
      final String targetName = targetInfoEntry.getKey();
      final TargetInfo targetInfo = targetInfoEntry.getValue();
      final DataNode<ModuleData> moduleDataNode = modules.get(targetName);
      if (targetInfo.isPythonTarget() && moduleDataNode != null) {
        final ModuleDependencyData moduleDependencyData = new ModuleDependencyData(
          moduleDataNode.getData(),
          requirementsModuleDataNode.getData()
        );
        moduleDependencyData.setExported(true);
        moduleDataNode.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData);
        moduleDataNode.createChild(
          PythonSetupData.KEY,
          new PythonSetupData(moduleDataNode.getData(), pythonSetup.getDefaultInterpreterInfo())
        );
      }
    }
  }

  @NotNull
  private DataNode<ModuleData> createRequirementsModule(
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull PythonSetup pythonSetup,
    @NotNull PantsCompileOptionsExecutor executor
  ) {
    final String moduleName = "python_requirements";
    final ModuleData moduleData = new ModuleData(
      moduleName,
      PantsConstants.SYSTEM_ID,
      ModuleTypeId.JAVA_MODULE,
      moduleName,
      projectDataNode.getData().getIdeProjectFileDirectoryPath() + "/" + moduleName,
      new File(executor.getBuildRoot(), moduleName).getAbsolutePath()
    );

    final DataNode<ModuleData> moduleDataNode = projectDataNode.createChild(ProjectKeys.MODULE, moduleData);

    final File chroot = new File(pythonSetup.getDefaultInterpreterInfo().getChroot());
    for (File dep : FileUtil.notNullize(new File(chroot, ".deps").listFiles())) {
      if (!dep.isDirectory()) {
        continue;
      }
      final ContentRootData contentRoot = new ContentRootData(PantsConstants.SYSTEM_ID, dep.getAbsolutePath());
      contentRoot.storePath(ExternalSystemSourceType.SOURCE, dep.getAbsolutePath());
      moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);
    }

    return moduleDataNode;
  }
}
