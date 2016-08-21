// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.projectview;

import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInManager;
import com.intellij.ide.StandardTargetWeights;
import com.intellij.ide.impl.ProjectViewSelectInTarget;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFileSystemItem;
import com.twitter.intellij.pants.util.PantsUtil;

import java.util.Optional;

public class PantsProjectPaneSelectInTarget extends ProjectViewSelectInTarget {
  protected PantsProjectPaneSelectInTarget(Project project) {
    super(project);
  }

  @Override
  public String getMinorViewId() {
    return ProjectFilesViewPane.ID;
  }

  @Override
  public float getWeight() {
    return StandardTargetWeights.PROJECT_WEIGHT;
  }

  @Override
  protected boolean canWorkWithCustomObjects() {
    return false;
  }

  @Override
  public boolean canSelect(PsiFileSystemItem file) {
    if (!super.canSelect(file)) return false;
    final VirtualFile vFile = file.getVirtualFile();
    return canSelect(vFile);
  }

  @Override
  public boolean isSubIdSelectable(String subId, SelectInContext context) {
    return canSelect(context);
  }

  private boolean canSelect(final VirtualFile vFile) {
    if (vFile != null && vFile.isValid()) {
      ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
      if (projectFileIndex.isInLibraryClasses(vFile) || projectFileIndex.isInLibrarySource(vFile)) {
        return true;
      }
      final Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(myProject.getBaseDir());

      return buildRoot.isPresent() && VfsUtil.isAncestor(buildRoot.get(), vFile, false);
    }

    return false;
  }

  public String toString() {
    return SelectInManager.PROJECT;
  }
}
