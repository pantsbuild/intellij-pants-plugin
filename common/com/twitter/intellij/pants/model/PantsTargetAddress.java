// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class PantsTargetAddress {
  private final String myPath;
  private final String myTargetName;

  public PantsTargetAddress(@NotNull String path, @NotNull String name) {
    myPath = path;
    myTargetName = name;
  }

  /**
   * @return relative or absolute path depending on how it was created. Suitable for both cases.
   */
  @NotNull
  public String getPath() {
    return myPath;
  }

  @NotNull
  public String getRelativePath() {
    final File buildRoot = PantsUtil.findBuildRoot(new File(getPath()));
    final String relativePath = buildRoot != null ? PantsUtil.getRelativeProjectPath(buildRoot, getPath()) : null;
    return StringUtil.notNullize(relativePath, getPath());
  }

  @NotNull
  public String getTargetName() {
    return myTargetName;
  }

  @Override
  public String toString() {
    return myPath + ":" + myTargetName;
  }

  @NotNull
  public static PantsTargetAddress fromString(@NotNull @NonNls String targetName) {
    final PantsTargetAddress result = fromString(targetName, false);
    assert result != null;
    return result;
  }

  public boolean isMainTarget() {
    return StringUtil.equals(PathUtil.getFileName(getPath()), getTargetName());
  }

  /**
   * @param strict - if <code>true</code> the method will return <code>null</code> if there is no <code>:</code> indicating a target name.
   */
  @Nullable
  public static PantsTargetAddress fromString(@NotNull @NonNls String targetPath, boolean strict) {
    final int index = targetPath.lastIndexOf(':');
    if (index < 0) {
      return strict ? null : new PantsTargetAddress(targetPath, PathUtil.getFileName(targetPath));
    } else {
      final String path = targetPath.substring(0, index);
      final String name = targetPath.substring(index + 1);
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
