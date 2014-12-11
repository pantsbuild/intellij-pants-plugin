// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PantsCompileOptions {
  @NotNull
  String getTargetPath();

  @NotNull
  List<String> getTargetNames();

  boolean isAllTargets();
}
