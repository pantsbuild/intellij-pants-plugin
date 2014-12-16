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
  private boolean myCompileWithIntellij;
  private String myTargetPath;
  private List<String> myTargetNames;

  public JpsPantsProjectExtensionImpl(@NotNull String path, @NotNull List<String> names, boolean compileWithIntellij) {
    myTargetPath = path;
    myTargetNames = names;
    myCompileWithIntellij = compileWithIntellij;
  }

  @NotNull
  @Override
  public JpsPantsProjectExtensionImpl createCopy() {
    return new JpsPantsProjectExtensionImpl(myTargetPath, myTargetNames, myCompileWithIntellij);
  }

  @Override
  public void applyChanges(@NotNull JpsPantsProjectExtensionImpl modified) {
    setTargetPath(modified.getTargetPath());
    setTargetNames(modified.getTargetNames());
    setCompileWithIntellij(modified.isCompileWithIntellij());
  }

  @Override
  public boolean isAllTargets() {
    return myTargetNames.isEmpty();
  }

  @NotNull
  @Override
  public String getTargetPath() {
    return myTargetPath;
  }

  @Override
  public void setTargetPath(@NotNull String path) {
    myTargetPath = PantsUtil.isBUILDFilePath(path) ? PathUtil.getParentPath(path) : path;
  }

  @NotNull
  @Override
  public List<String> getTargetNames() {
    return myTargetNames;
  }

  @Override
  public void setTargetNames(@NotNull List<String> names) {
    myTargetNames = names;
  }

  @Override
  public boolean isCompileWithIntellij() {
    return myCompileWithIntellij;
  }

  public void setCompileWithIntellij(boolean compileWithIntellij) {
    myCompileWithIntellij = compileWithIntellij;
  }
}
