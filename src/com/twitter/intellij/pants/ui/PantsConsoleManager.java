// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.twitter.intellij.pants.util.PantsConstants;

import java.util.concurrent.ConcurrentHashMap;


public class PantsConsoleManager {
  private static ConcurrentHashMap<Project, ConsoleView> mapper = new ConcurrentHashMap<>();

  public static void registerConsole(Project project) {
    //ConsoleView executionConsole = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
    //mapper.put(project, executionConsole);
    //ApplicationManager.getApplication().invokeAndWait(new Runnable() {
    //  @Override
    //  public void run() {
    //    ToolWindow window = ToolWindowManager.getInstance(project)
    //      .getToolWindow("PantsConsole");
    //    //window.getComponent().add(executionConsole.getComponent());
    //  }
    //});
  }

  public static void registerConsole(Project project, ConsoleView console) {
    mapper.put(project, console);
  }

  public static ConsoleView getConsole(Project project) {
    return mapper.get(project);
  }

  public static void unregisterConsole(Project project) {
    mapper.remove(project);
  }
}
