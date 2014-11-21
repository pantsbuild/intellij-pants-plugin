// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PantsTargetAddress {
  private final String myRelativePath;
  private final String myTargetName;

  public PantsTargetAddress(@NotNull String path, @NotNull String name) {
    myRelativePath = path;
    myTargetName = name;
  }

  @NotNull
  public String getRelativePath() {
    return myRelativePath;
  }

  @NotNull
  public String getTargetName() {
    return myTargetName;
  }

  @Override
  public String toString() {
    return myRelativePath + ":" + myTargetName;
  }

  @NotNull
  public static PantsTargetAddress fromString(@NotNull @NonNls String targetName) {
    int index = targetName.lastIndexOf(':');
    if (index < 0) {
      //
      return new PantsTargetAddress(targetName, PathUtil.getFileName(targetName));
    } else {
      final String path = targetName.substring(0, index);
      final String name = targetName.substring(index + 1);
      return new PantsTargetAddress(path, StringUtil.isEmpty(name) ? PathUtil.getFileName(path) : name);
    }
  }
}
