// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.google.gson.JsonSyntaxException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.model.SimpleExportResult;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.model.graph.BuildGraph;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PantsResolver {
  /**
   * Bump this version if project resolve changes. It will prompt user to refresh.
   * E.g. more modules are created or their relationship changes.
   *
   * 16: with 'import dep as jar', allow all target types (except Python targets) to depend on libraries directly.
   *     Previously only JarLibrary targets can depend on libraries.
   */
  public static final int VERSION = 16;

  protected static final Logger LOG = Logger.getInstance(PantsResolver.class);
  protected final PantsCompileOptionsExecutor myExecutor;
  protected ProjectInfo myProjectInfo = null;

  public PantsResolver(@NotNull PantsCompileOptionsExecutor executor) {
    myExecutor = executor;
  }

  public static ProjectInfo parseProjectInfoFromJSON(@NotNull String data) throws JsonSyntaxException {
    final int jsonStart = data.indexOf("\n{");
    if (jsonStart > 0) {
      data = data.substring(jsonStart + 1);
    }
    return ProjectInfo.fromJson(data);
  }

  @Nullable
  public ProjectInfo getProjectInfo() {
    return myProjectInfo;
  }

  @TestOnly
  public void setProjectInfo(ProjectInfo projectInfo) {
    myProjectInfo = projectInfo;
  }

  private void parse(final String output) {
    myProjectInfo = null;
    if (output.isEmpty()) throw new ExternalSystemException("Not output from pants");
    try {
      myProjectInfo = parseProjectInfoFromJSON(output);
    }
    catch (JsonSyntaxException e) {
      LOG.warn("Can't parse output\n" + output, e);
      throw new ExternalSystemException("Can't parse project structure!");
    }
  }

  public void resolve(
    @NotNull Consumer<String> statusConsumer,
    @Nullable ProcessAdapter processAdapter
  ) {
    try {
      String pantsExportResult = myExecutor.loadProjectStructure(statusConsumer, processAdapter);
      parse(pantsExportResult);
    }
    catch (ExecutionException | IOException e) {
      throw new ExternalSystemException(e);
    }
  }

  public void addInfoTo(@NotNull DataNode<ProjectData> projectInfoDataNode) {
    if (myProjectInfo == null) return;


    LOG.debug("Amount of targets before modifiers: " + myProjectInfo.getTargets().size());
    for (PantsProjectInfoModifierExtension modifier : PantsProjectInfoModifierExtension.EP_NAME.getExtensions()) {
      modifier.modify(myProjectInfo, myExecutor, LOG);
    }
    LOG.debug("Amount of targets after modifiers: " + myProjectInfo.getTargets().size());

    Optional<BuildGraph> buildGraph = constructBuildGraph(projectInfoDataNode);

    PropertiesComponent.getInstance().setValues(PantsConstants.PANTS_AVAILABLE_TARGETS_KEY, myProjectInfo.getAvailableTargetTypes());
    final Map<String, DataNode<ModuleData>> modules = new HashMap<>();
    for (PantsResolverExtension resolver : PantsResolverExtension.EP_NAME.getExtensions()) {
      resolver.resolve(myProjectInfo, myExecutor, projectInfoDataNode, modules, buildGraph);
    }
    if (LOG.isDebugEnabled()) {
      final int amountOfModules = PantsUtil.findChildren(projectInfoDataNode, ProjectKeys.MODULE).size();
      LOG.debug("Amount of modules created: " + amountOfModules);
    }
  }

  private Optional<BuildGraph> constructBuildGraph(@NotNull DataNode<ProjectData> projectInfoDataNode) {
    Optional<BuildGraph> buildGraph;
    if (myExecutor.getOptions().incrementalImportDepth().isPresent()) {
      Optional<VirtualFile> pantsExecutable = PantsUtil.findPantsExecutable(projectInfoDataNode.getData().getLinkedExternalProjectPath());
      SimpleExportResult result = SimpleExportResult.getExportResult(pantsExecutable.get().getPath());
      if (PantsUtil.versionCompare(result.getVersion(), "1.0.9") < 0) {
        throw new PantsException(PantsBundle.message("pants.resolve.incremental.import.unsupported"));
      }
      buildGraph = Optional.of(new BuildGraph(myProjectInfo.getTargets()));
    }
    else {
      buildGraph = Optional.empty();
    }
    return buildGraph;
  }
}
