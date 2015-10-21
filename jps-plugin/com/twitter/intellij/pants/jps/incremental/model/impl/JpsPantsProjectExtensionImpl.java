// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model.impl;

import com.twitter.intellij.pants.jps.incremental.model.JpsPantsProjectExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.ex.JpsElementBase;

public class JpsPantsProjectExtensionImpl extends JpsElementBase<JpsPantsProjectExtensionImpl> implements JpsPantsProjectExtension {
  private String myPantsExecutablePath;
  private boolean myCompileWithIntellij;
  private boolean myCompileWithDebugInfo;

  public JpsPantsProjectExtensionImpl(@NotNull String pantsExecutable, boolean compileWithIntellij, boolean compileWithDebugInfo) {
    myPantsExecutablePath = pantsExecutable;
    myCompileWithIntellij = compileWithIntellij;
    myCompileWithDebugInfo = compileWithDebugInfo;
  }

  @NotNull
  @Override
  public JpsPantsProjectExtensionImpl createCopy() {
    return new JpsPantsProjectExtensionImpl(myPantsExecutablePath, myCompileWithIntellij, myCompileWithDebugInfo);
  }

  @Override
  public void applyChanges(@NotNull JpsPantsProjectExtensionImpl modified) {
    setPantsExecutablePath(modified.getPantsExecutablePath());
    setCompileWithIntellij(modified.isCompileWithIntellij());
    setCompileWithDebugInfo(modified.isCompileWithDebugInfo());
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
  public boolean isCompileWithDebugInfo() {
    return myCompileWithDebugInfo;
  }

  @Override
  public void setCompileWithIntellij(boolean compileWithIntellij) {
    myCompileWithIntellij = compileWithIntellij;
  }

  @Override
  public void setCompileWithDebugInfo(boolean compileWithDebugInfo){ myCompileWithDebugInfo = compileWithDebugInfo;}
}
