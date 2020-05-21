// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).
package com.twitter.intellij.pants.ui;

import com.intellij.execution.ui.ConsoleView;
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

  protected Project myProject;

  public PantsConsoleViewPanel(Project project, ConsoleView console) {
    myProject = project;
    myConfiguration = ErrorTreeViewConfiguration.getInstance(project);
    setLayout(new BorderLayout());

    AutoScrollToSourceHandler autoScrollToSourceHandler = new AutoScrollToSourceHandler() {
      @Override
      protected boolean isAutoScrollMode() {
        return myConfiguration.isAutoscrollToSource();
      }

      @Override
      protected void setAutoScrollMode(boolean state) {
        myConfiguration.setAutoscrollToSource(state);
      }
    };

    JPanel myMessagePanel = new JPanel(new BorderLayout());

    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    Tree tree = createTree(treeModel);
    tree.getEmptyText().setText(IdeBundle.message("errortree.noMessages"));

    autoScrollToSourceHandler.install(tree);
    TreeUtil.installActions(tree);

    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    tree.setLargeModel(true);

    JScrollPane scrollPane = NewErrorTreeRenderer.install(tree);
    scrollPane.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));
    myMessagePanel.add(console.getComponent(), BorderLayout.CENTER);

    add(createToolbarPanel(), BorderLayout.WEST);

    add(myMessagePanel, BorderLayout.CENTER);

    EditSourceOnDoubleClickHandler.install(tree);
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

  private static class StopAction extends DumbAwareAction {
    public StopAction() {
      super(IdeBundle.message("action.stop"), null, AllIcons.Actions.Suspend);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      PantsMakeBeforeRun.terminatePantsProcess(e.getProject());
      PantsConsoleManager.getConsole(e.getProject()).print(PantsBundle.message("pants.command.terminated"), ConsoleViewContentType.ERROR_OUTPUT);
    }

    @Override
    public void update(AnActionEvent event) {
      // Make the action only clickable when there is an active Pants process.
      Presentation presentation = event.getPresentation();
      Project project = event.getProject();
      if (project == null) {
        return;
      }
      presentation.setEnabled(PantsMakeBeforeRun.hasActivePantsProcess(project));
    }
  }

  private JPanel createToolbarPanel() {
    AnAction closeMessageViewAction = new StopAction();

    DefaultActionGroup leftUpdateableActionGroup = new DefaultActionGroup();
    leftUpdateableActionGroup.add(closeMessageViewAction);

    JPanel toolbarPanel = new JPanel(new BorderLayout());
    ActionManager actionManager = ActionManager.getInstance();
    ActionToolbar leftToolbar = actionManager.createActionToolbar(ActionPlaces.COMPILER_MESSAGES_TOOLBAR, leftUpdateableActionGroup, false);
    toolbarPanel.add(leftToolbar.getComponent(), BorderLayout.WEST);

    return toolbarPanel;
  }
}
