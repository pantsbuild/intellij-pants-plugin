// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model.impl;

import com.twitter.intellij.pants.jps.incremental.model.JpsPantsProjectExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.ex.JpsElementBase;

public class JpsPantsProjectExtensionImpl extends JpsElementBase<JpsPantsProjectExtensionImpl> implements JpsPantsProjectExtension {
  private String myPantsExecutablePath;
  private boolean myCompileWithIntellij;
  private String myJdkPath;

  public JpsPantsProjectExtensionImpl(@NotNull String pantsExecutable, boolean compileWithIntellij, @Nullable String jdkPath) {
    myPantsExecutablePath = pantsExecutable;
    myCompileWithIntellij = compileWithIntellij;
    myJdkPath = jdkPath;
  }

  @NotNull
  @Override
  public JpsPantsProjectExtensionImpl createCopy() {
    return new JpsPantsProjectExtensionImpl(myPantsExecutablePath, myCompileWithIntellij, myJdkPath);
  }

  @Override
  public void applyChanges(@NotNull JpsPantsProjectExtensionImpl modified) {
    setPantsExecutablePath(modified.getPantsExecutablePath());
    setCompileWithIntellij(modified.isCompileWithIntellij());
  }

  @Nullable
  @Override
  public String getJdkPath() {
    return myJdkPath;
  }

  @NotNull
  @Override
  public String getPantsExecutablePath() {
    return myPantsExecutablePath;
  }

  @Override
  public void setPantsExecutablePath(@NotNull String path) {
    myPantsExecutablePath = path;
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
