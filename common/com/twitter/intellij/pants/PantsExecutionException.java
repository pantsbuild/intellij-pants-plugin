// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.process.UnixProcessManager;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public class PantsExecutionException extends PantsException {
  private final ProcessOutput myProcessOutput;
  private final String myCommand;

  public PantsExecutionException(@NotNull String message, @NotNull String command, @NotNull ProcessOutput processOutput) {
    super(message);
    myCommand = command;
    myProcessOutput = processOutput;
  }

  @NotNull
  public String getCommand() {
    return myCommand;
  }

  public boolean isTerminated() {
    // check if exit code contains SIGTERM bits
    // some scripts may return (exit code + 128) indicating some special conditions
    return (getProcessOutput().getExitCode() & UnixProcessManager.SIGTERM) == UnixProcessManager.SIGTERM;
  }

  @NotNull
  public ProcessOutput getProcessOutput() {
    return myProcessOutput;
  }

  @Override
  public String getMessage() {
    final String originalMessage = super.getMessage();
    if (ApplicationManager.getApplication().isUnitTestMode() || ApplicationManager.getApplication().isInternal()) {
      return originalMessage + "\n" + getExecutionDetails();
    }
    return originalMessage;
  }

  @NotNull
  public String getExecutionDetails() {
    return getCommand() + "\n" +
           "Exit code: " + getProcessOutput().getExitCode() + "\n" +
           getProcessOutput().getStdout() + "\n" +
           getProcessOutput().getStderr();
  }
}
