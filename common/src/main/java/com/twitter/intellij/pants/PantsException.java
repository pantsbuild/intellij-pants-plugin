// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;

public class PantsException extends ExternalSystemException {
  public PantsException(String message) {
    super(message);
  }

  public PantsException(ExecutionException ex) {
    super(ex);
  }
}

