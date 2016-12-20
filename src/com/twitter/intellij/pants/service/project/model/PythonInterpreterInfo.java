// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PythonInterpreterInfo {
  @NotNull
  private String binary;
  @NotNull
  private String chroot;

  @NotNull
  public String getBinary() {
    return binary;
  }

  public void setBinary(@NotNull String binary) {
    this.binary = binary;
  }

  @NotNull
  public String getChroot() {
    return chroot;
  }

  public void setChroot(@NotNull String chroot) {
    this.chroot = chroot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PythonInterpreterInfo info = (PythonInterpreterInfo)o;

    if (!binary.equals(info.binary)) return false;
    return chroot.equals(info.chroot);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      binary,
      chroot
    );
  }
}
