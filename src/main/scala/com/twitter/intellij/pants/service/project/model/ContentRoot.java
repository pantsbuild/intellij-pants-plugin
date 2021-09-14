// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.google.common.base.Objects;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ContentRoot implements Comparable<ContentRoot> {
  // FIXME: Change `source_root` argument to `content_root` once it is corrected in Pants.
  private String source_root;
  private String package_prefix;

  public ContentRoot(@NotNull String source_root, @NotNull String package_prefix) {
    this.source_root = source_root;
    this.package_prefix = package_prefix;
  }

  @NotNull
  public String getPackageRoot() {
    // source_root might contain '.' in the path.

    final boolean sourceRootMatchesPackage = StringUtil.endsWith(source_root.replace(File.separatorChar, '.'), package_prefix);
    return sourceRootMatchesPackage ?
           source_root.substring(0, source_root.length() - package_prefix.length()) :
           source_root;
  }

  @NotNull
  public String getRawSourceRoot() {
    return source_root;
  }

  @Nullable
  public String getPackagePrefix() {
    return StringUtil.nullize(package_prefix);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ContentRoot root = (ContentRoot) o;

    if (!package_prefix.equals(root.package_prefix)) return false;
    if (!source_root.equals(root.source_root)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
      source_root,
      package_prefix
    );
  }

  @Override
  public String toString() {
    return String.format(
      "ContentRoot{'source_root='%s', package_prefix='%s'}",
      source_root,
      package_prefix
    );
  }

  @Override
  public int compareTo(@NotNull ContentRoot o) {
    return StringUtil.naturalCompare(getRawSourceRoot(), o.getRawSourceRoot());
  }
}
