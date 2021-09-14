// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.projectview;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectViewImpl;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.components.PantsProjectCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class VirtualFileTreeNode extends ProjectViewNode<VirtualFile> {
  public VirtualFileTreeNode(
    @NotNull Project project,
    @NotNull VirtualFile virtualFile,
    @NotNull ViewSettings viewSettings
  ) {
    super(project, virtualFile, viewSettings);
  }

  @Nullable
  @Override
  public VirtualFile getVirtualFile() {
    return getValue();
  }

  @Override
  public int getWeight() {
    final ProjectView projectView = ProjectView.getInstance(myProject);
    final boolean foldersOnTop = projectView instanceof ProjectViewImpl && !((ProjectViewImpl)projectView).isFoldersAlwaysOnTop();
    return foldersOnTop && getValue().isDirectory() ? 20 : 0; // see PsiDirectoryNode.getWeight()
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    final VirtualFile myFile = getValue();
    return myFile.isDirectory() && VfsUtil.isAncestor(myFile, file, true);
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    final VirtualFile virtualFile = getValue();
    final PsiFile psiElement = virtualFile.isValid() ? psiManager.findFile(virtualFile) : null;
    if (psiElement instanceof PsiDirectory) {
      new PsiDirectoryNode(myProject, (PsiDirectory)psiElement, getSettings()).update(presentation);
    }
    else if (psiElement != null) {
      new PsiFileNode(myProject, psiElement, getSettings()).update(presentation);
    }
    else {
      presentation.setPresentableText(virtualFile.getName());
      presentation.setIcon(virtualFile.isDirectory() ? AllIcons.Nodes.Folder : virtualFile.getFileType().getIcon());
    }
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    final VirtualFile virtualFile = getValue();
    return ContainerUtil.mapNotNull(
      virtualFile.isValid() && virtualFile.isDirectory() ? virtualFile.getChildren() : VirtualFile.EMPTY_ARRAY,
      new Function<VirtualFile, AbstractTreeNode<?>>() {
        @Override
        public AbstractTreeNode<?> fun(VirtualFile file) {
          final PsiElement psiElement = file.isDirectory() ? psiManager.findDirectory(file) : psiManager.findFile(file);
          if (psiElement instanceof PsiDirectory && ModuleUtil.findModuleForPsiElement(psiElement) != null) {
            // PsiDirectoryNode doesn't render files outside of a project
            // let's use PsiDirectoryNode only for folders in a modules
            return new PsiDirectoryNode(myProject, (PsiDirectory)psiElement, getSettings());
          }
          else if (psiElement instanceof PsiFile) {
            return new PsiFileNode(myProject, (PsiFile)psiElement, getSettings());
          }
          else {
            return shouldShow(file) ? new VirtualFileTreeNode(myProject, file, getSettings()) : null;
          }
        }
      }
    );
  }

  private boolean shouldShow(@NotNull VirtualFile file) {
    if (!file.isValid()) {
      return false;
    }
    if (file.isDirectory()) {
      return shouldIncludeDirectory(file);
    }
    return !file.getName().startsWith(".");
  }

  private boolean shouldIncludeDirectory(VirtualFile dir) {
    final ViewSettings viewSettings = getSettings();
    if (viewSettings instanceof PantsViewSettings && ((PantsViewSettings)viewSettings).isShowOnlyLoadedFiles()) {
      final PantsProjectCache projectCache = PantsProjectCache.getInstance(getProject());
      return projectCache.folderContainsSourceRoot(dir);
    }
    return true;
  }
}
