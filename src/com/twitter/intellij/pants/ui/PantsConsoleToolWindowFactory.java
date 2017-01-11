// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.jetbrains.annotations.NotNull;

public class PantsConsoleToolWindowFactory implements ToolWindowFactory, DumbAware {

  @Override
  public void createToolWindowContent(
    @NotNull Project project, @NotNull ToolWindow toolWindow
  ) {
    ConsoleView console = PantsConsoleManager.getOrMakeNewConsole(project);
    toolWindow.getComponent().removeAll();
    toolWindow.getComponent().add(console.getComponent());
    console.print("Welcome to Pants project!", ConsoleViewContentType.SYSTEM_OUTPUT);
  }
}
