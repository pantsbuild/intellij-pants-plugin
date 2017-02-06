// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.ViewStructureAction;
import com.intellij.ide.errorTreeView.NewErrorTreeViewPanel;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.view.ExternalProjectsViewImpl;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.impl.ContentImpl;
import com.intellij.ui.content.impl.TabbedContentImpl;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ipnb.editor.IpnbEditorUtil;
import org.jetbrains.plugins.ipnb.editor.actions.IpnbRunCellBaseAction;

import javax.swing.JButton;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PantsConsoleToolWindowFactory extends AbstractExternalSystemToolWindowFactory {

  public PantsConsoleToolWindowFactory() {
    super(PantsConstants.SYSTEM_ID);
  }

  //private class HideWarningsAction extends ToggleAction implements DumbAware {
  //  public HideWarningsAction() {
  //    super(IdeBundle.message("action.hide.warnings"), null, AllIcons.General.HideWarnings);
  //  }
  //
  //  @Override
  //  public boolean isSelected(AnActionEvent event) {
  //    return isHideWarnings();
  //  }
  //
  //  @Override
  //  public void setSelected(AnActionEvent event, boolean flag) {
  //
  //  }
  //}
  @Override
  public void createToolWindowContent(
    @NotNull Project project, @NotNull ToolWindow toolWindow
  ) {
    ConsoleView console = PantsConsoleManager.getOrMakeNewConsole(project);
    //toolWindow.getComponent().add(console.getComponent());
    //toolWindow.getComponent().add(new JButton(PantsIcons.Icon));
    ////
    ////SimpleToolWindowPanel
    ////
    ////toolWindow.setTitle(myExternalSystemId.getReadableName());
    //ContentManager contentManager = toolWindow.getContentManager();
    //final SimpleToolWindowPanel projectsView = new SimpleToolWindowPanel(true, true);

    PantsConsoleViewPanel helpme = new PantsConsoleViewPanel(project, "helpme");

    Content contentHelpMe = ContentFactory.SERVICE.getInstance().createContent(helpme.getComponent(), "helpme", true);
    //Content contentHelpMe = ContentFactory.SERVICE.getInstance().createContent(helpme.getComponent(), "helpme", true);
    toolWindow.getContentManager().addContent(contentHelpMe);
    //toolWindow.getContentManager().addContent(new TabbedContentImpl(console.getComponent(), "displayName", false, "title"));
    //ExternalProjectsManager.getInstance(project).registerView(projectsView);
    //ContentImpl tasksContent = new ContentImpl(projectsView, ExternalSystemBundle.message("tool.window.title.projects"), true);
    //toolWindow.getComponent().add(projectsView);
  }
}
