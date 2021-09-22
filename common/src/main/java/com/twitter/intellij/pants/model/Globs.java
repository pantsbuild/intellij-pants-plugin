// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class Globs {
  public static final Globs EMPTY = new Globs();

  protected List<String> globs;

  @NotNull
  public List<String> getGlobs() {
    return globs != null ? globs : Collections.<String>emptyList();
  }

  public void setGlobs(List<String> globs) {
    this.globs = globs;
  }

  public boolean hasFileExtension(@NotNull final String extension) {
    return ContainerUtil.exists(
      getGlobs(),
      new Condition<String>() {
        @Override
        public boolean value(String glob) {
          return StringUtil.equalsIgnoreCase(extension, PathUtil.getFileExtension(glob));
        }
      }
    );
  }
}
