package com.twitter.intellij.pants.projectView;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Created by ajohnson on 7/30/14.
 */
public class PantsTreeStructureProvider implements TreeStructureProvider {
  @NotNull
  @Override
  public Collection<AbstractTreeNode> modify(
    final AbstractTreeNode node, Collection<AbstractTreeNode> collection, ViewSettings settings
  ) {

    final Project project = node.getProject();
    if (node instanceof PsiDirectoryNode && project != null) {
      final Module module = ContainerUtil.find(
        ModuleManager.getInstance(project).getModules(), new Condition<Module>() {
          @Override
          public boolean value(Module module) {
            VirtualFile moduleFile = module.getModuleFile();
            return moduleFile != null && moduleFile.getParent().equals(((PsiDirectoryNode)node).getVirtualFile());
          }
        }
      );
      String value = module != null ? module.getOptionValue(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY) : null;
      if (value != null) {
        value = node.getProject().getBaseDir().getParent().getPath() + "/" + value;
        VirtualFile buildFile = LocalFileSystem.getInstance().findFileByPath(value);
        if (buildFile != null) {
          PsiFile buildPsiFile = PsiManager.getInstance(node.getProject()).findFile(buildFile);
          PsiFileNode buildNode = new PsiFileNode(node.getProject(), buildPsiFile, settings);
          collection.add(buildNode);
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
