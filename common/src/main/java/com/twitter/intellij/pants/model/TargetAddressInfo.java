// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TargetAddressInfo {
  /**
   * Target addresses.
   */
  private String targetAddress;

  /**
   * Target type.
   */
  protected String target_type;
  /**
   * Target type.
   */
  protected Globs globs;

  /**
   * Pants target type
   */
  private String pants_target_type = null;

  private boolean is_synthetic;

  public boolean isSynthetic() {
    return is_synthetic;
  }

  public void setIsTargetRoot(boolean is_target_root) {
    this.is_target_root = is_target_root;
  }

  public boolean isTargetRoot() {
    return is_target_root;
  }

  private boolean is_target_root;

  private String id;

  public TargetAddressInfo() {
  }

  public String getId() {
    return id;
  }

  @NotNull
  public Globs getGlobs() {
    return globs != null ? globs : Globs.EMPTY;
  }

  public void setGlobs(Globs globs) {
    this.globs = globs;
  }

  @Nullable
  public String getTargetType() {
    return target_type;
  }

  public void setTargetType(@NotNull String target_type) {
    this.target_type = target_type;
  }

  public String getTargetAddress() {
    return targetAddress;
  }

  public void setTargetAddress(String targetAddress) {
    this.targetAddress = targetAddress;
  }

  @Nullable
  public String getInternalPantsTargetType() {
    return pants_target_type;
  }

  @VisibleForTesting
  public void setPantsTargetType(@NotNull String type) {
    pants_target_type = type;
  }

  public boolean isScala() {
    return StringUtil.contains("scala", StringUtil.notNullize(getInternalPantsTargetType())) ||
           getGlobs().hasFileExtension("scala");
  }

  public boolean isPython() {
    return StringUtil.contains("python", StringUtil.notNullize(getInternalPantsTargetType())) ||
           getGlobs().hasFileExtension("py");
  }

  public boolean isJarLibrary() {
    return StringUtil.equals("jar_library", getInternalPantsTargetType());
  }

  public boolean isTargetAlias() { return pants_target_type != null && (pants_target_type.equals("alias") || pants_target_type.equals("target"));}
}
