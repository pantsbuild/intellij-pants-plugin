// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.HashSet;

public class TargetAddressInfo {
  /**
   * Constant type for gson to figure out the data type to deserialize
   */
  public static final Type TYPE = new TypeToken<HashSet<TargetAddressInfo>>(){}.getType();

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

  private boolean is_code_gen;


  private boolean is_synthetic;

  private String id;

  public TargetAddressInfo() {
  }

  public boolean is_synthetic() {
    return is_synthetic;
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

  public boolean isAnnotationProcessor() {
    return StringUtil.equals("annotation_processor", getInternalPantsTargetType());
  }

  public boolean isJarLibrary() {
    return StringUtil.equals("jar_library", getInternalPantsTargetType());
  }

  public String getCanonicalId() {
    if (getId() != null) {
      return getId();
    }
    else {
      return PantsUtil.getCanonicalTargetId(targetAddress);
    }
  }
}
