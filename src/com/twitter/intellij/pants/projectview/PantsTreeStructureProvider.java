// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.projectview;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PantsTreeStructureProvider implements TreeStructureProvider {
  @NotNull
  @Override
  public Collection<AbstractTreeNode> modify(
    @NotNull final AbstractTreeNode node,
    @NotNull Collection<AbstractTreeNode> collection,
    ViewSettings settings
  ) {
    final Project project = node.getProject();
    if (node instanceof PsiDirectoryNode && project != null) {
      final Module module = ModuleUtil.findModuleForPsiElement(((PsiDirectoryNode)node).getValue());
      final String buildPath = module != null ? PantsUtil.getPathFromAddress(module, ExternalSystemConstants.LINKED_PROJECT_PATH_KEY) : null;
      if (buildPath != null) {
        final VirtualFile buildFile = PantsUtil.findFileRelativeToPantsWorkingDir(project, buildPath);

        boolean isModuleRoot =
          ArrayUtil.contains(((PsiDirectoryNode)node).getVirtualFile(), ModuleRootManager.getInstance(module).getContentRoots());
        if (buildFile != null && isModuleRoot) {
          // Check if there's already a BUILD file in the directory; if so, we don't add another
          final AbstractTreeNode existingBuildFile = ContainerUtil.find(
            collection.iterator(), new Condition<AbstractTreeNode>() {
              @Override
              public boolean value(AbstractTreeNode node) {
                return node instanceof PsiFileNode && buildFile.equals(((PsiFileNode)node).getVirtualFile());
              }
            }
          );
          if (existingBuildFile == null) {
            final PsiFile buildPsiFile = PsiManager.getInstance(project).findFile(buildFile);
            final PsiFileNode buildNode = new PsiFileNode(project, buildPsiFile, settings) {
              @Override
              protected void updateImpl(PresentationData data) {
                super.updateImpl(data);
                data.setIcon(PantsIcons.Icon);
              }
            };
            final List<AbstractTreeNode> modifiedCollection = new ArrayList<AbstractTreeNode>(collection);
            modifiedCollection.add(buildNode);
            return modifiedCollection;
          }
        }
      }
    }
    return collection;
  }

  @Nullable
  @Override
  public Object getData(Collection<AbstractTreeNode> collection, String s) {
    return null;
  }
}
