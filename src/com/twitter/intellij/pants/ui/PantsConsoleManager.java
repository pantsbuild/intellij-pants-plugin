// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.TestOnly;

import java.util.concurrent.ConcurrentHashMap;


public class PantsConsoleManager {
  private static ConcurrentHashMap<Project, ConsoleView> mapper = new ConcurrentHashMap<>();

  public static void registerConsole(Project project) {
    getOrMakeNewConsole(project);
  }

  /**
   * Creates a `ConsoleView` for the current project, and register it under `PantsConsole` tool window,
   * or just retrieve one if there is already one registered.
   *
   * @param project current project
   * @return Pants ConsoleView for the project
   */
  public static ConsoleView getOrMakeNewConsole(Project project) {
    ConsoleView console = mapper.get(project);
    if (console != null) {
      return console;
    }
    ConsoleView newConsole = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
    mapper.put(project, newConsole);
    Disposer.register(project, newConsole);
    return newConsole;
  }

  /**
   * Close the console for a project.
   *
   * @param project current project
   */
  public static void unregisterConsole(Project project) {
    mapper.remove(project);
  }

  /**
   * TestOnly because some test library is not tearing down properly.
   */
  @TestOnly
  public static void disposeAll() {
    //for (Map.Entry<Project, ConsoleView> entrySet : mapper.entrySet()) {
    //  entrySet.getValue().dispose();
    //}
  }
}
