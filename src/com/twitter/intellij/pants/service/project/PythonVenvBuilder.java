// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.diagnostic.Logger;
import com.twitter.intellij.pants.service.project.model.PythonInterpreterInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class PythonVenvBuilder {

  private final Path executable;

  private static final String executableName = "venv_builder.pex";

  protected static final Logger LOG = Logger.getInstance(PythonVenvBuilder.class);

  private PythonVenvBuilder(Path executable) {
    this.executable = executable;
  }

  public PythonInterpreterInfo build(String target, Path venvDir){
    LOG.info(String.format("Invoking the .venv builder with target %s at %s", target, venvDir));
    try {
      Process process = new ProcessBuilder(executable.toString(),
                                           "--target", target,
                                           "--venv-dir", venvDir.toString(),
                                           "--pip"
      ).start();
      process.waitFor();
      BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
      LOG.info("Output of the .venv builder:");
      output.lines().forEach(l -> LOG.info("  " + l));
      PythonInterpreterInfo result = new PythonInterpreterInfo();
      result.setBinary(venvDir.resolve("bin/python").toString());
      result.setChroot(venvDir.toString());
      return result;
    } catch(IOException | InterruptedException ie) {
      throw new RuntimeException("An error occurred while running the .venv builder", ie);
    }
  }

  public static Optional<PythonVenvBuilder> find() {
    Path executable = Paths.get(System.getProperty("user.home"), "workspace", "source", "dist", executableName); //FIXME
    if(Files.isRegularFile(executable) && Files.isExecutable(executable))
      return Optional.of(new PythonVenvBuilder(executable));
    else
      return Optional.empty();
  }

}
