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
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ajohnson on 7/30/14.
 */
public class PantsTreeStructureProvider implements TreeStructureProvider {
  @NotNull
  @Override
  public Collection<AbstractTreeNode> modify(
    final AbstractTreeNode node,
    Collection<AbstractTreeNode> collection,
    ViewSettings settings
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
      String buildPath = module != null ? module.getOptionValue(ExternalSystemConstants.LINKED_PROJECT_PATH_KEY) : null;
      if (buildPath != null) {
        buildPath = node.getProject().getBaseDir().getParent().getPath() + "/" + buildPath;
        VirtualFile buildFile = LocalFileSystem.getInstance().findFileByPath(buildPath);
        if (buildFile != null) {
          PsiFile buildPsiFile = PsiManager.getInstance(node.getProject()).findFile(buildFile);
          AddedPsiFileNode buildNode = new AddedPsiFileNode(node.getProject(), buildPsiFile, settings);
          // Check if there's already a BUILD file in the directory; if so, we don't add another
          Iterator iterator = collection.iterator();
          while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next instanceof PsiFileNode) {
              if (((PsiFileNode) next).getVirtualFile().getName().equals("BUILD")) {
                return collection;
              }
            }
          }
          if (!collection.contains(buildNode)) {
            List<AbstractTreeNode> modifiedCollection = new ArrayList<AbstractTreeNode>(collection);
            modifiedCollection.add(buildNode);
            return modifiedCollection;
          }
        }
      }
    }

    if (node instanceof AddedPsiFileNode) {
      if (((PsiFileNode)node).getTitle().contains("BUILD")) {
        node.setIcon(PantsIcons.Icon);
      }
    }
    return collection;
  }

  @Nullable
  @Override
  public Object getData(Collection<AbstractTreeNode> collection, String s) {
    return null;
  }

  /*
  This class is only used for BUILD files injected into project structure view by the plugin; these files are marked with the Pants logo to
  differentiate them.
   */
  private class AddedPsiFileNode extends PsiFileNode {
    public AddedPsiFileNode(Project project, PsiFile value, ViewSettings viewSettings) {
      super(project, value, viewSettings);
    }
  }
}
