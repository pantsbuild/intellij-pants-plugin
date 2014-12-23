// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsTargetAddress {
  private final String myRelativePath;
  private final String myTargetName;

  public PantsTargetAddress(@NotNull String path, @NotNull String name) {
    myRelativePath = path;
    myTargetName = name;
  }

  /**
   * @return relative or absolute path depending on how it was created. Suitable for both cases.
   */
  @NotNull
  public String getPath() {
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
    final PantsTargetAddress result = fromString(targetName, false);
    assert result != null;
    return result;
  }

  /**
   * @param strict - if <code>true</code> the method will return <code>null</code> if there is no <code>:</code> indicating a target name.
   */
  @Nullable
  public static PantsTargetAddress fromString(@NotNull @NonNls String targetName, boolean strict) {
    final int index = targetName.lastIndexOf(':');
    if (index < 0) {
      return strict ? null : new PantsTargetAddress(targetName, PathUtil.getFileName(targetName));
    } else {
      final String path = targetName.substring(0, index);
      final String name = targetName.substring(index + 1);
      return new PantsTargetAddress(path, StringUtil.isEmpty(name) ? PathUtil.getFileName(path) : name);
    }
  }

  @Contract(value = "null -> null", pure = true)
  public static String extractPath(@Nullable String address) {
    if (StringUtil.isEmpty(address)) {
      return null;
    }
    final int index = address.lastIndexOf(':');
    return index < 0 ? address : address.substring(0, index);
  }
}
