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
  private boolean myWithDependees;
  private String myTargetPath;
  private List<String> myTargetNames;

  public JpsPantsProjectExtensionImpl(@NotNull String path, @NotNull List<String> names, boolean withDependees, boolean compileWithIntellij) {
    myTargetPath = path;
    myTargetNames = names;
    myWithDependees = withDependees;
    myCompileWithIntellij = compileWithIntellij;
  }

  @NotNull
  @Override
  public JpsPantsProjectExtensionImpl createCopy() {
    return new JpsPantsProjectExtensionImpl(myTargetPath, myTargetNames, myWithDependees, myCompileWithIntellij);
  }

  @Override
  public void applyChanges(@NotNull JpsPantsProjectExtensionImpl modified) {
    setExternalProjectPath(modified.getExternalProjectPath());
    setTargetNames(modified.getTargetNames());
    setCompileWithIntellij(modified.isCompileWithIntellij());
    setWithDependees(modified.isWithDependees());
  }

  @Override
  public boolean isWithDependees() {
    return myWithDependees;
  }

  @Override
  public void setWithDependees(boolean withDependees) {
    myWithDependees = withDependees;
  }

  @NotNull
  @Override
  public String getExternalProjectPath() {
    return myTargetPath;
  }

  @Override
  public void setExternalProjectPath(@NotNull String path) {
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

  @Override
  public void setCompileWithIntellij(boolean compileWithIntellij) {
    myCompileWithIntellij = compileWithIntellij;
  }
}
