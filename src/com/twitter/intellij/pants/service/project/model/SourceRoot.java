// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.model.PantsSourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SourceRoot {
  protected String source_root;
  protected String package_prefix;

  public SourceRoot() {
  }

  public SourceRoot(@NotNull String source_root, String package_prefix) {
    this.source_root = source_root;
    this.package_prefix = package_prefix;
  }

  @NotNull
  public String getSourceRootRegardingSourceType(@Nullable PantsSourceType rootType) {
    if (PantsSourceType.isResource(rootType)) {
      final String resourcesPath = StringUtil.replaceChar(package_prefix, '.', '/');
      return source_root.endsWith(resourcesPath) ?
             source_root.substring(0, source_root.length() - resourcesPath.length()) :
             source_root;
    }
    else {
      return source_root;
    }
  }

  public String getPackagePrefix() {
    return package_prefix;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SourceRoot root = (SourceRoot)o;

    if (package_prefix != null ? !package_prefix.equals(root.package_prefix) : root.package_prefix != null) return false;
    if (source_root != null ? !source_root.equals(root.source_root) : root.source_root != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = source_root != null ? source_root.hashCode() : 0;
    result = 31 * result + (package_prefix != null ? package_prefix.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "SourceRoot{" +
           "source_root='" + source_root + '\'' +
           ", package_prefix='" + package_prefix + '\'' +
           '}';
  }
}
