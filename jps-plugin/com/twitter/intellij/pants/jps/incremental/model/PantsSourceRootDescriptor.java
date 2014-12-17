// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.BuildRootDescriptor;

import java.io.File;
import java.util.Set;

public class PantsSourceRootDescriptor extends BuildRootDescriptor {
  @NotNull
  public final File myRoot;
  public final boolean myGeneratedSources;
  private final Set<File> myExcludes;
  private final PantsBuildTarget myTarget;

  public PantsSourceRootDescriptor(
    @NotNull PantsBuildTarget target,
    @NotNull File root,
    boolean isGenerated,
    @NotNull Set<File> excludes
  ) {
    myTarget = target;
    myRoot = root;
    myGeneratedSources = isGenerated;
    myExcludes = excludes;
  }

  @NotNull
  @Override
  public Set<File> getExcludedRoots() {
    return myExcludes;
  }

  @Override
  public String getRootId() {
    return FileUtil.toSystemIndependentName(myRoot.getPath());
  }

  @Override
  public File getRootFile() {
    return myRoot;
  }

  @Override
  public PantsBuildTarget getTarget() {
    return myTarget;
  }

  @Override
  public boolean isGenerated() {
    return myGeneratedSources;
  }

  @Override
  public boolean canUseFileCache() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PantsSourceRootDescriptor that = (PantsSourceRootDescriptor)o;

    if (myGeneratedSources != that.myGeneratedSources) return false;
    if (!myExcludes.equals(that.myExcludes)) return false;
    if (!FileUtil.filesEqual(myRoot, that.myRoot)) return false;
    if (!myTarget.equals(that.myTarget)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = FileUtil.fileHashCode(myRoot);
    result = 31 * result + (myGeneratedSources ? 1 : 0);
    result = 31 * result + myExcludes.hashCode();
    result = 31 * result + myTarget.hashCode();
    return result;
  }
}
