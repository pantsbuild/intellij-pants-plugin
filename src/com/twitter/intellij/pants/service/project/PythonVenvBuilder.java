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
import java.util.List;
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
    Collection<String> allTargets = PantsUtil.listMatchingTargets(projectPath, "entsec/venv_builder:venv_builder");
    if(!allTargets.isEmpty())
      return Optional.of(new PythonVenvBuilder(projectPath, processAdapter));
    else
      return Optional.empty();
  }

  public PythonInterpreterInfo build(List<String> targets, Path venvDir) {
    LOG.info(String.format("Building a Python virtual environment with targets %s at %s", targets, venvDir));
    try {
      GeneralCommandLine command = buildAndRunVenvBuilder(targets, venvDir);
      LOG.info(String.format("Executing command: %s", command.getCommandLineString()));
      final ProcessOutput processOutput = getProcessOutput(command);
      if(processOutput.checkSuccess(LOG)) {
        PythonInterpreterInfo result = new PythonInterpreterInfo();
        result.setBinary(venvDir.resolve("bin/python").toString());
        result.setChroot(venvDir.toString());
        return result;
      }
      else {
        String message = String.format("Failed to create a Python virtual environment in %s", venvDir);
        LOG.error(message);
        LOG.error("  ----- Beginning of the stderr output");
        processOutput.getStderrLines().forEach(line -> {
          LOG.error(line);
        });
        LOG.error("  ----- End of the stderr output");
        throw new RuntimeException(message);
      }
    } catch(ExecutionException ee) {
      throw new RuntimeException("An error occurred while running the .venv builder", ee);
    }
  }

  private GeneralCommandLine buildAndRunVenvBuilder(List<String> targets, Path venvDir) {
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(projectPath)
      .withEnvironment("PANTS_CONCURRENT", "true");
    commandLine.addParameters("run", "entsec/venv_builder:venv_builder", "--");
    for(String target: targets){
      commandLine.addParameters("--target", target);
    }
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
