// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.projectview;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.ProjectViewImpl;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

public class TargetSpecsFileTreeNode extends ProjectViewNode<VirtualFile> {
  private final Boolean myIsRoot;

  public TargetSpecsFileTreeNode(
    @NotNull Project project,
    @NotNull VirtualFile virtualFile,
    @NotNull ViewSettings viewSettings,
    @NotNull Boolean isRoot
  ) {
    super(project, virtualFile, viewSettings);
    myIsRoot = isRoot;
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
    return foldersOnTop && getValue().isDirectory() ? 20 : 0;
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
    PsiDirectory psiFile = psiManager.findDirectory(virtualFile);
    Module module = psiFile != null ? ModuleUtil.findModuleForPsiElement(psiFile) : null;


    String path = myIsRoot ? PantsUtil.getRelativeProjectPath(virtualFile.getPath()).orElse(virtualFile.getName()) : virtualFile.getName();
    presentation.clearText();
    presentation.addText(path, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    if(module != null) {
      boolean moduleRoot = Arrays.asList(ModuleRootManager.getInstance(module).getContentRoots()).contains(virtualFile);
      if(moduleRoot) {
        presentation.addText(" [" + module.getName() + "]", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      }
    }
    presentation.setIcon(virtualFile.isDirectory() ? AllIcons.Nodes.Folder : virtualFile.getFileType().getIcon());
  }

  @NotNull
  @Override
  public Collection<? extends AbstractTreeNode<?>> getChildren() {
    final VirtualFile virtualFile = getValue();
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    VirtualFile[] files = virtualFile.isValid() && virtualFile.isDirectory()
                          ? Arrays.stream(virtualFile.getChildren()).sorted(Comparator.comparing(VirtualFile::getName))
                            .toArray(VirtualFile[]::new)
                          : VirtualFile.EMPTY_ARRAY;
    return ContainerUtil.mapNotNull(
      files,
      (Function<VirtualFile, AbstractTreeNode<?>>) file -> {
        final PsiElement psiElement = file.isDirectory() ? psiManager.findDirectory(file) : psiManager.findFile(file);
        if (psiElement instanceof PsiDirectory) {
          return new TargetSpecsFileTreeNode(myProject, file, getSettings(), false);
        }
        else if(psiElement != null) {
          return new PsiFileNode(myProject, (PsiFile) psiElement, getSettings());
        } else {
          return null;
        }
      }
    );
  }
}
