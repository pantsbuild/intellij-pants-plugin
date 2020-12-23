// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.twitter.intellij.pants.service.project.model.PythonInterpreterInfo;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public class PythonVenvBuilder {

  protected static final Logger LOG = Logger.getInstance(PythonVenvBuilder.class);

  private final String projectPath;
  private final ProcessAdapter processAdapter;

  private PythonVenvBuilder(String projectPath, ProcessAdapter processAdapter) {
    this.projectPath = projectPath;
    this.processAdapter = processAdapter;
  }

  public static Optional<PythonVenvBuilder> forProjectPath(String projectPath, ProcessAdapter processAdapter){
    Collection<String> allTargets = PantsUtil.listAllTargets(projectPath);
    if(allTargets.contains("entsec/venv_builder:venv_builder"))
      return Optional.of(new PythonVenvBuilder(projectPath, processAdapter));
    else
      return Optional.empty();
  }

  public PythonInterpreterInfo build(String target, Path venvDir) {
    LOG.info(String.format("Invoking the .venv builder with target %s at %s", target, venvDir));
    try {
      GeneralCommandLine command = buildAndRunVenvBuilder(target, venvDir);
      final ProcessOutput processOutput = getProcessOutput(command);
      if(processOutput.checkSuccess(LOG)) {
        PythonInterpreterInfo result = new PythonInterpreterInfo();
        result.setBinary(venvDir.resolve("bin/python").toString());
        result.setChroot(venvDir.toString());
        return result;
      }
      else {
        throw new RuntimeException(String.format("Failed to create a Python virtual environment in %s", venvDir));
      }
    } catch(ExecutionException ee) {
      throw new RuntimeException("An error occurred while running the .venv builder", ee);
    }
  }

  private GeneralCommandLine buildAndRunVenvBuilder(String target, Path venvDir) {
    //TODO Use PantsTaskManager.executeTasks?
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(projectPath)
      .withEnvironment("PANTS_CONCURRENT", "true");
    commandLine.addParameters("run", "entsec/venv_builder:venv_builder", "--");
    commandLine.addParameters("--target", target);
    commandLine.addParameters("--venv-dir", venvDir.toString());
    commandLine.addParameters("--pip");
    return commandLine;
  }

  private ProcessOutput getProcessOutput(
    @NotNull GeneralCommandLine command
  ) throws ExecutionException {
    //Copied from PantsCompileOptionsExecutor
    final Process process = command.createProcess();
    return PantsUtil.getCmdOutput(process, command.getCommandLineString(), processAdapter);
  }

}
