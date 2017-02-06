// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

public class PantsConsoleToolWindowFactory extends AbstractExternalSystemToolWindowFactory {

  public PantsConsoleToolWindowFactory() {
    super(PantsConstants.SYSTEM_ID);
  }

  @Override
  public void createToolWindowContent(
    @NotNull Project project, @NotNull ToolWindow toolWindow
  ) {
    PantsConsoleViewPanel pantsConsoleViewPanel = new PantsConsoleViewPanel(project);
    Content contentHelpMe = ContentFactory.SERVICE.getInstance().createContent(pantsConsoleViewPanel, "pantsConsoleViewPanel", true);
    toolWindow.getContentManager().addContent(contentHelpMe);
  }
}
