// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TargetInfo {
  public TargetInfo() {
  }

  public TargetInfo(
    Set<String> libraries,
    Set<String> targets,
    Set<SourceRoot> roots,
    String target_type,
    Boolean is_code_gen
  ) {
    this.libraries = libraries;
    this.targets = targets;
    this.roots = roots;
    this.target_type = target_type;
    this.is_code_gen = is_code_gen;
  }

  /**
   * List of libraries. Just names.
   */
  protected Set<String> libraries;
  /**
   * List of dependencies.
   */
  protected Set<String> targets;
  /**
   * List of source roots.
   */
  protected Set<SourceRoot> roots;
  /**
   * Target type.
   */
  protected String target_type;

  /**
   * Pants target type
   */
  private String pants_target_type = null;

  private Boolean is_code_gen;

  public Set<String> getLibraries() {
    return libraries;
  }

  public void setLibraries(Set<String> libraries) {
    this.libraries = libraries;
  }

  public Set<String> getTargets() {
    return targets;
  }

  public void setTargets(Set<String> targets) {
    this.targets = targets;
  }

  public Set<SourceRoot> getRoots() {
    return roots;
  }

  public void setRoots(Set<SourceRoot> roots) {
    this.roots = roots;
  }

  @Nullable
  public String getTargetType() {
    return target_type;
  }

  @Nullable
  public void setTargetType(@NotNull String target_type) {
    this.target_type = target_type;
  }

  @Nullable
  public String getInternalPantsTargetType() {
    return pants_target_type;
  }

  public boolean isEmpty() {
    return libraries.isEmpty() && targets.isEmpty() && roots.isEmpty();
  }

  public boolean isCodeGen() {
    return is_code_gen;
  }

  public boolean isScalaTarget() {
    return StringUtil.equals("scala_library", getInternalPantsTargetType());
  }

  public boolean dependOn(@NotNull String targetName) {
    return targets.contains(targetName);
  }

  @NotNull
  public PantsSourceType getSourcesType() {
    return PantsUtil.getSourceTypeForTargetType(getTargetType());
  }

  public void addDependency(@NotNull String targetName) {
    getTargets().add(targetName);
  }

  public boolean removeDependency(@NotNull String targetName) {
    return getTargets().remove(targetName);
  }

  public void replaceDependency(@NotNull String targetName, @NotNull String newTargetName) {
    if (removeDependency(targetName)) {
      addDependency(newTargetName);
    }
  }

  public TargetInfo intersect(@NotNull TargetInfo other) {
    final Collection<String> libs = ContainerUtil.intersection(getLibraries(), other.getLibraries());
    final Collection<String> targets = ContainerUtil.intersection(getTargets(), other.getTargets());
    final Collection<SourceRoot> roots = ContainerUtil.intersection(getRoots(), other.getRoots());
    return new TargetInfo(
      new HashSet<String>(libs),
      new HashSet<String>(targets),
      new HashSet<SourceRoot>(roots),
      getTargetType(),
      is_code_gen
    );
  }

  public TargetInfo union(@NotNull TargetInfo other) {
    return new TargetInfo(
      ContainerUtil.union(getLibraries(), other.getLibraries()),
      ContainerUtil.union(getTargets(), other.getTargets()),
      ContainerUtil.union(getRoots(), other.getRoots()),
      getTargetType(),
      is_code_gen
    );
  }

  @Override
  public String toString() {
    return "TargetInfo{" +
           "libraries=" + libraries +
           ", targets=" + targets +
           ", roots=" + roots +
           ", target_type='" + target_type + '\'' +
           ", is_code_gen=" + is_code_gen +
           '}';
  }
}
