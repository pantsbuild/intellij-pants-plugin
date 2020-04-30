// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.projectview;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PantsTreeStructureProvider implements TreeStructureProvider {
  @NotNull
  @Override
  public Collection<AbstractTreeNode<?>> modify(
    @NotNull final AbstractTreeNode<?> node,
    @NotNull Collection<AbstractTreeNode<?>> collection,
    ViewSettings settings
  ) {
    Project project = node.getProject();
    if (project == null || !(node instanceof PsiDirectoryNode)) return collection;
    PsiDirectoryNode directory = (PsiDirectoryNode) node;

    List<PsiFileNode> newNodes =
      Optional.ofNullable(getModuleOf(directory))
        .filter(module -> isModuleRoot(directory, module))
        .flatMap(PantsUtil::findModuleAddress)
        .flatMap(buildPAth -> PantsUtil.findFileRelativeToBuildRoot(project, buildPAth))
        .filter(buildFile -> !alreadyExists(collection, buildFile))
        .map(buildFile -> createNode(settings, project, buildFile))
        .orElseGet(Collections::emptyList);

    if (newNodes.isEmpty()) return collection;

    return Stream.concat(collection.stream(), newNodes.stream()).collect(Collectors.toList());
  }

  private Module getModuleOf(@NotNull PsiDirectoryNode node) {
    return ModuleUtil.findModuleForPsiElement(node.getValue());
  }

  private boolean alreadyExists(Collection<AbstractTreeNode<?>> collection, VirtualFile buildFile) {
    Condition<AbstractTreeNode<?>> isBuildFile =
      node -> node instanceof PsiFileNode && buildFile.equals(((PsiFileNode) node).getVirtualFile());
    return ContainerUtil.exists(collection, isBuildFile);
  }

  @NotNull
  private List<PsiFileNode> createNode(ViewSettings settings, Project project, VirtualFile buildFile) {
    final PsiFile buildPsiFile = PsiManager.getInstance(project).findFile(buildFile);
    if(buildPsiFile == null) return Collections.emptyList();

    PsiFileNode node = new PsiFileNode(project, buildPsiFile, settings) {
      @Override
      protected void updateImpl(@NotNull PresentationData data) {
        super.updateImpl(data);
        data.setIcon(PantsIcons.Icon);
      }
    };
    return Collections.singletonList(node);
  }

  private boolean isModuleRoot(@NotNull PsiDirectoryNode node, Module module) {
    return ArrayUtil.indexOf(ModuleRootManager.getInstance(module).getContentRoots(), node.getVirtualFile()) >= 0;
  }

  @Nullable
  @Override
  public Object getData(@NotNull Collection<AbstractTreeNode<?>> collection, @NotNull String s) {
    return null;
  }
}
