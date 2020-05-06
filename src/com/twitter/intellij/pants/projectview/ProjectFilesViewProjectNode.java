// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.projectview;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.projectView.impl.nodes.AbstractProjectNode;
import com.intellij.ide.projectView.impl.nodes.ExternalLibrariesNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class ProjectFilesViewProjectNode extends AbstractProjectNode {

  public ProjectFilesViewProjectNode(Project project, ViewSettings viewSettings) {
    super(project, project, viewSettings);
  }

  @Override
  protected AbstractTreeNode<?> createModuleGroup(@NotNull Module module)
    throws NoSuchMethodException {
    // should be never called
    throw new NoSuchMethodException(PantsBundle.message("pants.error.not.implemented"));
  }

  @Override
  protected AbstractTreeNode<?> createModuleGroupNode(@NotNull ModuleGroup moduleGroup)
    throws NoSuchMethodException {
    // should be never called
    throw new NoSuchMethodException(PantsBundle.message("pants.error.not.implemented"));
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    final Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(myProject);
    final VirtualFile projectDir = buildRoot.orElse(myProject.getBaseDir());
    if (projectDir == null) {
      LOG.warn(String.format("Couldn't find project directory for project '%s'", myProject.getName()));
      return Collections.emptyList();
    }
    final AbstractTreeNode<?> root = new VirtualFileTreeNode(myProject, projectDir, getSettings());
    if (getSettings().isShowLibraryContents()) {
      return Arrays.asList(
        root,
        new ExternalLibrariesNode(getProject(), getSettings())
      );
    }
    return Collections.singletonList(root);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    final Optional<VirtualFile> projectBuildRoot = PantsUtil.findBuildRoot(myProject.getBaseDir());
    return super.contains(file) ||
           (projectBuildRoot.isPresent() && VfsUtil.isAncestor(projectBuildRoot.get(), file, true));
  }
}
