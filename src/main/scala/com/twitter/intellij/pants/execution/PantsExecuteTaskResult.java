// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import java.util.Optional;

/**
 * Wrapper class describing the result of a pants invocation.
 */
public class PantsExecuteTaskResult {

  public final boolean succeeded;
  public final Optional<String> output;

  public PantsExecuteTaskResult(final boolean succeeded, final Optional<String> output) {
    this.succeeded = succeeded;
    this.output = output;
  }

  public static PantsExecuteTaskResult emptyFailure() {
    return new PantsExecuteTaskResult(false, Optional.empty());
  }
}
