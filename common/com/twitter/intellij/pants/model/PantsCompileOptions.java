// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import org.jetbrains.annotations.NotNull;

public interface PantsCompileOptions extends PantsExecutionOptions {
  @NotNull
  String getExternalProjectPath();
}
