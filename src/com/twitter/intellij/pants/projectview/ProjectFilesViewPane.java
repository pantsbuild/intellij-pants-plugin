package com.twitter.intellij.pants.projectview;

import com.intellij.icons.AllIcons;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.impl.ProjectPaneSelectInTarget;
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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class ProjectFilesViewPane extends AbstractProjectViewPSIPane {
  @NonNls public static final String ID = "ProjectFilesPane";

  public ProjectFilesViewPane(Project project) {
    super(project);
  }

  @Override
  public String getTitle() {
    return PantsBundle.message("pants.title.project.files");
  }

  @Override
  public Icon getIcon() {
    return AllIcons.General.ProjectTab;
  }

  @Override
  @NotNull
  public String getId() {
    return ID;
  }

  @Override
  public int getWeight() {
    // There are 5 project view panels with ids from 1 to 5.
    // There is an assertion that id should be unique. See ProjectViewImpl.java:441
    // Picked an id that I like. :-)
    return 239;
  }

  @Override
  protected ProjectAbstractTreeStructureBase createStructure() {
    return new ProjectTreeStructure(myProject, ID){
      @Override
      protected AbstractTreeNode createRoot(final Project project, ViewSettings settings) {
        return new ProjectFilesViewProjectNode(project, settings);
      }
    };
  }

  @Override
  protected ProjectViewTree createTree(DefaultTreeModel treeModel) {
    return new ProjectViewTree(myProject, treeModel) {
      @Override
      public DefaultMutableTreeNode getSelectedNode() {
        return ProjectFilesViewPane.this.getSelectedNode();
      }
    };
  }

  @Override
  protected AbstractTreeUpdater createTreeUpdater(AbstractTreeBuilder treeBuilder) {
    return new AbstractTreeUpdater(treeBuilder);
  }

  @Override
  public SelectInTarget createSelectInTarget() {
    return new PantsProjectPaneSelectInTarget(myProject);
  }
}
