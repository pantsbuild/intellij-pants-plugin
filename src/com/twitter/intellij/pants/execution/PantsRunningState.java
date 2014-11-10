// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PantsRunningState extends CommandLineState {

  private final PantsRunnerParameters runnerParameters;

  public PantsRunningState(ExecutionEnvironment environment, PantsRunnerParameters runnerParameters) {
    super(environment);
    this.runnerParameters = runnerParameters;
    addConsoleFilters(new PantsFilter(environment.getProject()));
  }

  @NotNull
  @Override
  protected ProcessHandler startProcess() throws ExecutionException {
    GeneralCommandLine commandLine = getCommand();

    final OSProcessHandler processHandler = new ColoredProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
    ProcessTerminatedListener.attach(processHandler, getEnvironment().getProject());
    return processHandler;
  }

  public GeneralCommandLine getCommand() throws ExecutionException {
    final GeneralCommandLine commandLine = new GeneralCommandLine();

    final String exePath = runnerParameters.getExecutable();
    if (exePath == null) {
      throw new ExecutionException("Invalid pants executable path");
    }

    commandLine.setExePath(exePath);
    commandLine.setPassParentEnvironment(true);

    final String cmdArguments = StringUtil.nullize(runnerParameters.getArguments());
    final List<String> arguments = cmdArguments != null ? ParametersListUtil.parse(cmdArguments) : Collections.<String>emptyList();
    for (String argument : arguments) {
      commandLine.addParameter(argument);
    }

    return commandLine.withWorkDirectory(runnerParameters.getWorkingDir());
  }
}
