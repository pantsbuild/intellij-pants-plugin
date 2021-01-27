// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.ide.util.PropertiesComponent;
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
    Collection<String> allTargets = PantsUtil.listMatchingTargets(projectPath, venvBuilderTarget());
    if(!allTargets.isEmpty())
      return Optional.of(new PythonVenvBuilder(projectPath, processAdapter));
    else
      return Optional.empty();
  }

  public PythonInterpreterInfo build(List<String> targets, Path venvDir) {
    LOG.info(String.format("Invoking the .venv builder with targets %s at %s", targets, venvDir));
    try {
      GeneralCommandLine command = buildAndRunVenvBuilder(targets, venvDir);
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

  private GeneralCommandLine buildAndRunVenvBuilder(List<String> targets, Path venvDir) {
    //TODO Use PantsTaskManager.executeTasks?
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(projectPath)
      .withEnvironment("PANTS_CONCURRENT", "true");
    commandLine.addParameters("run", venvBuilderTarget(), "--");
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

  private static String venvBuilderTarget() {
    String targetName = Optional.ofNullable(PropertiesComponent.getInstance().getValue("pants.python.venv_builder.target"))
      //FIXME replace with "entsec/venv_builder:venv_builder"
      .orElse("entsec/foobar:venv_builder");
    LOG.info(String.format("The venv_builder tool is assumed to be at %s", targetName));
    return targetName;
  }

}
