// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).
package com.twitter.intellij.pants.ui;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.errorTreeView.NewErrorTreeRenderer;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.ide.errorTreeView.impl.ErrorTreeViewConfiguration;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AutoScrollToSourceHandler;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SideBorder;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.EditSourceOnDoubleClickHandler;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;

/**
 * A JPanel consisting control buttons on the left and a ConsoleView on the right displaying Pants output.
 * <p>
 * Template came from {@link NewErrorTreeViewPanel} but heavily trimmed.
 */
public class PantsConsoleViewPanel extends JPanel {
  protected static final Logger LOG = Logger.getInstance("#com.intellij.ide.errorTreeView.NewErrorTreeViewPanel");
  private final ErrorTreeViewConfiguration myConfiguration;

  private ActionToolbar myLeftToolbar;
  protected Project myProject;
  protected Tree myTree;
  private final JPanel myMessagePanel;

  private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;


  public PantsConsoleViewPanel(Project project) {
    myProject = project;
    myConfiguration = ErrorTreeViewConfiguration.getInstance(project);
    setLayout(new BorderLayout());

    myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
      @Override
      protected boolean isAutoScrollMode() {
        return myConfiguration.isAutoscrollToSource();
      }

      @Override
      protected void setAutoScrollMode(boolean state) {
        myConfiguration.setAutoscrollToSource(state);
      }
    };

    myMessagePanel = new JPanel(new BorderLayout());

    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    myTree = createTree(treeModel);
    myTree.getEmptyText().setText(IdeBundle.message("errortree.noMessages"));

    myAutoScrollToSourceHandler.install(myTree);
    TreeUtil.installActions(myTree);
    UIUtil.setLineStyleAngled(myTree);
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    myTree.setLargeModel(true);

    JScrollPane scrollPane = NewErrorTreeRenderer.install(myTree);
    scrollPane.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));
    myMessagePanel.add(PantsConsoleManager.getOrMakeNewConsole(myProject).getComponent(), BorderLayout.CENTER);

    add(createToolbarPanel(), BorderLayout.WEST);

    add(myMessagePanel, BorderLayout.CENTER);

    EditSourceOnDoubleClickHandler.install(myTree);
  }

  @NotNull
  protected Tree createTree(@NotNull final DefaultTreeModel treeModel) {
    return new Tree(treeModel) {
      @Override
      public void setRowHeight(int i) {
        super.setRowHeight(0);
        // this is needed in order to make UI calculate the height for each particular row
      }
    };
  }

  private class StopAction extends DumbAwareAction {
    public StopAction() {
      super(IdeBundle.message("action.stop"), null, AllIcons.Actions.Suspend);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      PantsMakeBeforeRun.terminatePantsProcess(e.getProject());
      PantsConsoleManager.getOrMakeNewConsole(e.getProject()).print(PantsBundle.message("pants.command.terminated"), ConsoleViewContentType.ERROR_OUTPUT);
    }

    @Override
    public void update(AnActionEvent event) {
      // Make the action only clickable when there is active Pants proecess.
      Presentation presentation = event.getPresentation();
      presentation.setEnabled(PantsMakeBeforeRun.hasActivePantsProcess(event.getProject()));
    }
  }

  private JPanel createToolbarPanel() {
    AnAction closeMessageViewAction = new StopAction();

    DefaultActionGroup leftUpdateableActionGroup = new DefaultActionGroup();
    leftUpdateableActionGroup.add(closeMessageViewAction);

    JPanel toolbarPanel = new JPanel(new BorderLayout());
    ActionManager actionManager = ActionManager.getInstance();
    myLeftToolbar = actionManager.createActionToolbar(ActionPlaces.COMPILER_MESSAGES_TOOLBAR, leftUpdateableActionGroup, false);
    toolbarPanel.add(myLeftToolbar.getComponent(), BorderLayout.WEST);

    return toolbarPanel;
  }
}
