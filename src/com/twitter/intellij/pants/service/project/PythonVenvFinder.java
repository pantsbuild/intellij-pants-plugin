// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.diagnostic.Logger;
import com.twitter.intellij.pants.service.project.model.PythonInterpreterInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class PythonVenvFinder {

  private Path baseDir;

  private Optional<String> version = Optional.empty();
  private Optional<PythonInterpreterInfo> environment = Optional.empty();

  private static final String venvDirectoryName = ".venv";
  private static final String configName = "pyvenv.cfg";
  private static final String binaryName = "bin/python";
  private static final String versionPrefix = "version = ";

  protected static final Logger LOG = Logger.getInstance(PythonVenvFinder.class);

  public PythonVenvFinder(Path baseDir) {
    this.baseDir = baseDir;
    find();
  }

  private void find() {
    Path venv = baseDir.resolve(venvDirectoryName);
    if(Files.isDirectory(venv)) {
      LOG.info(String.format("Looking for a Python environment in %s", venv));
      Path config = venv.resolve(configName);
      Path binary = venv.resolve(binaryName);
      if(Files.isRegularFile(config) && Files.isRegularFile(binary)) {
        try {
          version = Files.lines(config)
            .filter(l -> l.startsWith(versionPrefix))
            .map(l -> l.substring(versionPrefix.length()))
            .findFirst();
        } catch (IOException e) {
          LOG.warn(String.format("An error occurred while reading %s", config), e);
        }
        LOG.info(String.format("Found Python version %s at %s", version.orElse("unknown"), venv));

        PythonInterpreterInfo result = new PythonInterpreterInfo();
        result.setBinary(venv.resolve(binaryName).toString());
        result.setChroot(venv.toString());
        environment = Optional.of(result);
      }
    }
  }

  public Path getBaseDir() {
    return baseDir;
  }

  public Optional<String> getVersion() {
    return version;
  }

  public Optional<PythonInterpreterInfo> getEnvironment() {
    return environment;
  }
}
