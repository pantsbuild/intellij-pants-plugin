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

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;

public class ProjectFilesViewProjectNode extends AbstractProjectNode {

  public ProjectFilesViewProjectNode(Project project, ViewSettings viewSettings) {
    super(project, project, viewSettings);
  }

  @Override
  protected AbstractTreeNode createModuleGroup(Module module)
    throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
    // should be never called
    throw new NoSuchMethodException(PantsBundle.message("pants.error.not.implemented"));
  }

  @Override
  protected AbstractTreeNode createModuleGroupNode(ModuleGroup moduleGroup)
    throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
    // should be never called
    throw new NoSuchMethodException(PantsBundle.message("pants.error.not.implemented"));
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode> getChildren() {
    final VirtualFile workingDir = PantsUtil.findPantsWorkingDir(myProject.getBaseDir());
    final AbstractTreeNode root =
      new VirtualFileTreeNode(myProject, workingDir != null ? workingDir : myProject.getBaseDir(), getSettings());
    if (getSettings().isShowLibraryContents()) {
      return Arrays.asList(
        root,
        new ExternalLibrariesNode(getProject(), getSettings())
      );
    }
    return Arrays.asList(root);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    final VirtualFile projectWorkingDir = PantsUtil.findPantsWorkingDir(myProject.getBaseDir());
    return super.contains(file) ||
           (projectWorkingDir != null && VfsUtil.isAncestor(projectWorkingDir, file, true));
  }
}
