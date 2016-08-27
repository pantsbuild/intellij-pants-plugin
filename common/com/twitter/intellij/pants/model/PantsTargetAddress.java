// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;

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
    final Optional<File> buildRoot = PantsUtil.findBuildRoot(new File(getPath()));
    return buildRoot
      .flatMap(file -> PantsUtil.getRelativeProjectPath(file, getPath()))
      .orElse(getPath());
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
    final Optional<PantsTargetAddress> result = fromString(targetName, false);
    assert result.isPresent();
    return result.get();
  }

  public boolean isMainTarget() {
    return StringUtil.equals(PathUtil.getFileName(getPath()), getTargetName());
  }

  /**
   * @param strict - if <code>true</code> the method will return <code>Optional.empty()</code> if there is no <code>:</code> indicating a target name.
   */
  public static Optional<PantsTargetAddress> fromString(@NotNull @NonNls String targetPath, boolean strict) {
    final int index = targetPath.lastIndexOf(':');
    if (index < 0) {
      return strict ? Optional.empty() : Optional.of(new PantsTargetAddress(targetPath, PathUtil.getFileName(targetPath)));
    } else {
      final String path = targetPath.substring(0, index);
      final String name = targetPath.substring(index + 1);
      return Optional.of(new PantsTargetAddress(path, StringUtil.isEmpty(name) ? PathUtil.getFileName(path) : name));
    }
  }

  public static Optional<String> extractPath(@Nullable String address) {
    if (StringUtil.isEmpty(address)) {
      return Optional.empty();
    }
    final int index = address.lastIndexOf(':');
    return Optional.of(index < 0 ? address : address.substring(0, index));
  }
}
