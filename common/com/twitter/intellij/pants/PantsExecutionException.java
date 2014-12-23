// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants;

import com.intellij.execution.process.ProcessOutput;

public class PantsExecutionException extends PantsException {
  private final ProcessOutput myProcessOutput;

  public PantsExecutionException(String message, ProcessOutput processOutput) {
    super(message);
    myProcessOutput = processOutput;
  }

  public ProcessOutput getProcessOutput() {
    return myProcessOutput;
  }
}
