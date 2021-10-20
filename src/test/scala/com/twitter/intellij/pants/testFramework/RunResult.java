// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import java.util.List;
import java.util.stream.Collectors;

public final class RunResult {
  private final int exitCode;
  private final List<String> output;
  private final List<String> errorOutput;

  RunResult(int exitCode, List<String> output, List<String> errorOutput) {
    this.exitCode = exitCode;
    this.output = output;
    this.errorOutput = errorOutput;
  }

  public int getExitCode() {
    return exitCode;
  }

  @Override
  public String toString() {
    return "exit code: " + exitCode +
           "\nOUT:\n" + join(output) +
           "\nERR:\n" + join(errorOutput);
  }

  private String join(List<String> list) {
    return list.stream().map(text -> "\t" + text).collect(Collectors.joining(""));
  }
}
