// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PythonSetup {
  @NotNull
  private String default_interpreter;
  @NotNull
  private Map<String, PythonInterpreterInfo> interpreters;

  @NotNull
  public PythonInterpreterInfo getDefaultInterpreterInfo() {
    return getInterpreters().get(getDefaultInterpreter());
  }

  @NotNull
  public String getDefaultInterpreter() {
    return default_interpreter;
  }

  public void setDefaultInterpreter(@NotNull String defaultInterpreter) {
    default_interpreter = defaultInterpreter;
  }

  @NotNull
  public Map<String, PythonInterpreterInfo> getInterpreters() {
    return interpreters;
  }

  public void setInterpreters(@NotNull Map<String, PythonInterpreterInfo> interpreters) {
    this.interpreters = interpreters;
  }
}
