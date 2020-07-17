// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.projectview;

import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.AbstractProjectViewPSIPane;
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import com.intellij.ide.projectView.impl.ProjectTreeStructure;
import com.intellij.ide.projectView.impl.ProjectViewTree;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeUpdater;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.PantsBundle;
import icons.PantsIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class TargetSpecsViewPane extends AbstractProjectViewPSIPane {
  @NonNls public static final String ID = "TargetSpecsViewPane";
  private boolean myShowExcludedFiles = true;
  private boolean myShowOnlyLoadedFiles = false;

  public TargetSpecsViewPane(Project project) {
    super(project);
  }

  @NotNull
  @Override
  public String getTitle() {
    return PantsBundle.message("pants.title.target.spec.files");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return PantsIcons.Icon;
  }

  @Override
  @NotNull
  public String getId() {
    return ID;
  }

  @Override
  public int getWeight() {
    return 240;
  }

  @NotNull
  @Override
  protected ProjectAbstractTreeStructureBase createStructure() {
    return new ProjectViewPaneTreeStructure();
  }

  @NotNull
  @Override
  protected ProjectViewTree createTree(@NotNull DefaultTreeModel treeModel) {
    return new ProjectViewTree(myProject, treeModel) {
      @Override
      public DefaultMutableTreeNode getSelectedNode() {
        return TargetSpecsViewPane.this.getSelectedNode();
      }
    };
  }

 @NotNull
 @Override
 protected AbstractTreeUpdater createTreeUpdater(@NotNull AbstractTreeBuilder treeBuilder) {
   return new AbstractTreeUpdater(treeBuilder);
 }

  @NotNull
  @Override
  public SelectInTarget createSelectInTarget() {
    return new TargetSpecsSelectInTarget(myProject);
  }

  private class ProjectViewPaneTreeStructure extends ProjectTreeStructure implements PantsViewSettings {
    public ProjectViewPaneTreeStructure() {
      super(TargetSpecsViewPane.this.myProject, ID);
    }

    @Override
    protected AbstractTreeNode<?> createRoot(@NotNull final Project project, @NotNull ViewSettings settings) {
      return new TargetSpecsViewProjectNode(project, settings);
    }

    @Override
    public boolean isShowExcludedFiles() {
      return myShowExcludedFiles;
    }

    @Override
    public boolean isShowOnlyLoadedFiles() {
      return myShowOnlyLoadedFiles;
    }
  }

}
