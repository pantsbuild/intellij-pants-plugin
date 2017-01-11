// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.impl.TabbedContentImpl;
import com.twitter.intellij.pants.util.PantsConstants;

import java.util.concurrent.ConcurrentHashMap;


public class PantsConsoleManager {
  private static ConcurrentHashMap<Project, ConsoleView> mapper = new ConcurrentHashMap<>();

  public static void registerConsole(Project project) {
    ToolWindow window =
      ToolWindowManager.getInstance(project).registerToolWindow(
        PantsConstants.PANTS_CONSOLE_NAME,
        true,
        ToolWindowAnchor.BOTTOM,
        project,
        true
      );
    ConsoleView console = getOrMakeNewConsole(project);
    Disposer.register(project, console);
    TabbedContentImpl content = new TabbedContentImpl(console.getComponent(), "", true, "");
    window.getContentManager().addContent(content);
  }

  public static ConsoleView getOrMakeNewConsole(Project project) {
    ConsoleView console = mapper.get(project);
    if (console != null) {
      return console;
    }
    ConsoleView newConsole = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
    mapper.put(project, newConsole);
    return newConsole;
  }

  public static void unregisterConsole(Project project) {
    mapper.remove(project);
  }
}
