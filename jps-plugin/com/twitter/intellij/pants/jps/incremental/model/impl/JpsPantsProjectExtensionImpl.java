// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model.impl;

import com.intellij.util.PathUtil;
import com.twitter.intellij.pants.jps.incremental.model.JpsPantsProjectExtension;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementBase;

import java.util.List;

public class JpsPantsProjectExtensionImpl extends JpsElementBase<JpsPantsProjectExtensionImpl> implements JpsPantsProjectExtension {
  private String targetPath;
  private List<String> targetNames;

  public JpsPantsProjectExtensionImpl(@NotNull String path, @NotNull List<String> names) {
    targetPath = path;
    targetNames = names;
  }

  @NotNull
  @Override
  public JpsPantsProjectExtensionImpl createCopy() {
    return new JpsPantsProjectExtensionImpl(targetPath, targetNames);
  }

  @Override
  public void applyChanges(@NotNull JpsPantsProjectExtensionImpl modified) {
    setTargetPath(modified.getTargetPath());
    setTargetNames(modified.getTargetNames());
  }

  @Override
  public boolean isAllTargets() {
    return targetNames.isEmpty();
  }

  @NotNull
  @Override
  public String getTargetPath() {
    return targetPath;
  }

  @Override
  public void setTargetPath(@NotNull String path) {
    targetPath = PantsUtil.isBUILDFilePath(path) ? PathUtil.getParentPath(path) : path;
  }

  @NotNull
  @Override
  public List<String> getTargetNames() {
    return targetNames;
  }

  @Override
  public void setTargetNames(@NotNull List<String> names) {
    targetNames = names;
  }
}
