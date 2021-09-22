// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;

public class PantsConsoleManager {
  private final ConsoleView myConsole;

  public PantsConsoleManager(Project project) {
    myConsole = createNewConsole(project);

    Disposer.register(project, myConsole); // for some reason extending Disposable results in leaked resources
    initializeConsolePanel(project, myConsole);
  }

  private static ConsoleView createNewConsole(Project project) {
    return TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
  }

  public static ConsoleView getConsole(Project project) {
    PantsConsoleManager service = ServiceManager.getService(project, PantsConsoleManager.class);
    return service.getConsole();
  }

  private static void initializeConsolePanel(Project project, ConsoleView console) {
    ToolWindow window =
      ToolWindowManager.getInstance(project).registerToolWindow(
        PantsConstants.PANTS_CONSOLE_NAME,
        true,
        ToolWindowAnchor.BOTTOM,
        project,
        true
      );

    window.setIcon(PantsIcons.Icon);

    PantsConsoleViewPanel pantsConsoleViewPanel = new PantsConsoleViewPanel(project, console);
    final boolean isLockable = true;
    final String displayName = "";
    Content pantsConsoleContent = ContentFactory.SERVICE.getInstance().createContent(pantsConsoleViewPanel, displayName, isLockable);
    pantsConsoleContent.setCloseable(false);
    window.getContentManager().addContent(pantsConsoleContent);
  }

  public ConsoleView getConsole() {
    return myConsole;
  }
}
