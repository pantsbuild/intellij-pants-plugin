// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.projectview;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.projectView.impl.nodes.AbstractProjectNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.bsp.FastpassUtils;
import com.twitter.intellij.pants.bsp.PantsBspData;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TargetSpecsViewProjectNode extends AbstractProjectNode {

  public TargetSpecsViewProjectNode(Project project, ViewSettings viewSettings) {
    super(project, project, viewSettings);
  }

  @NotNull
  @Override
  protected AbstractTreeNode<?> createModuleGroup(@NotNull Module module)
    throws NoSuchMethodException {
    // should be never called
    throw new NoSuchMethodException(PantsBundle.message("pants.error.not.implemented"));
  }

  @NotNull
  @Override
  protected AbstractTreeNode<?> createModuleGroupNode(@NotNull ModuleGroup moduleGroup)
    throws NoSuchMethodException {
    // should be never called
    throw new NoSuchMethodException(PantsBundle.message("pants.error.not.implemented"));
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    Set<VirtualFile> topLevelNodes = PantsUtil.isFastpassProject(myProject)
                                      ? fastpassTargetSpecViewTopLevelNodes(myProject)
                                      : PantsUtil.isPantsProject(myProject)
                                        ? regularPantsTargetSpecViewTopLevelNodes(myProject)
                                        : Collections.emptySet();
    return topLevelNodes
      .stream()
      .sorted(Comparator.comparing(VirtualFile::toString))
      .map(file -> new TargetSpecsFileTreeNode(myProject, file, getSettings(), true))
      .collect(Collectors.toList());
  }

  private Set<VirtualFile> fastpassTargetSpecViewTopLevelNodes(Project project) {
    Set<PantsBspData> linkedProjects = PantsBspData.importsFor(project);
    Set<CompletableFuture<Set<VirtualFile>>> futures = linkedProjects.stream().map(pantsBspData -> {
      VirtualFile pantsRoot = pantsBspData.getPantsRoot();
      return FastpassUtils.selectedTargets(pantsBspData).thenApply(
        targetSpecs -> targetSpecs.stream()
          .map(targetSpecsItem -> pantsRoot.findFileByRelativePath(PantsTargetAddress.fromString(targetSpecsItem).getPath().toString()))
          .collect(Collectors.toSet())
      );
    }).collect(Collectors.toSet());
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(
      __ -> futures.stream().flatMap(x -> x.join().stream()).collect(Collectors.toSet())
    ).join();
  }

  @NotNull
  private Set<VirtualFile> regularPantsTargetSpecViewTopLevelNodes(Project project) {
    AbstractExternalSystemSettings<?, ?, ?> systemSettings =
      ExternalSystemApiUtil.getSettings(project, PantsConstants.SYSTEM_ID);
    List<PantsTargetAddress> roots =
      systemSettings.getLinkedProjectsSettings().stream()
        .flatMap(s -> ((PantsProjectSettings) (s)).getSelectedTargetSpecs().stream())
        .map(PantsTargetAddress::fromString)
        .collect(Collectors.toList());
    return roots.stream().map(address -> LocalFileSystem.getInstance().findFileByNioFile(address.getPath()))
      .collect(Collectors.toSet());
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    final Optional<VirtualFile> projectBuildRoot = PantsUtil.findBuildRoot(myProject.getBaseDir());
    return super.contains(file) ||
           (projectBuildRoot.isPresent() && VfsUtil.isAncestor(projectBuildRoot.get(), file, true));
  }
}
