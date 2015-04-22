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
    String internal_target_type,
    Boolean is_code_gen
  ) {
    this(libraries, targets, roots, target_type, internal_target_type, is_code_gen, new HashSet<String>());
  }

  public TargetInfo(
    Set<String> libraries,
    Set<String> targets,
    Set<SourceRoot> roots,
    String target_type,
    String internal_target_type,
    Boolean is_code_gen,
    Set<String> targetAddresses
  ) {
    this.libraries = libraries;
    this.targets = targets;
    this.roots = roots;
    this.target_type = target_type;
    this.pants_target_type = internal_target_type;
    this.is_code_gen = is_code_gen;
    myTargetAddresses = targetAddresses;
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
   * Target addresses.
   */
  private Set<String> myTargetAddresses;

  /**
   * Pants target type
   */
  private String pants_target_type = null;

  private Boolean is_code_gen;

  @NotNull
  public Set<String> getLibraries() {
    return libraries;
  }

  public void setLibraries(Set<String> libraries) {
    this.libraries = libraries;
  }

  @NotNull
  public Set<String> getTargets() {
    return targets;
  }

  public void setTargets(Set<String> targets) {
    this.targets = targets;
  }

  @NotNull
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

  public void setTargetType(@NotNull String target_type) {
    this.target_type = target_type;
  }

  @NotNull
  public Set<String> getTargetAddresses() {
    return myTargetAddresses;
  }

  public void setTargetAddresses(Set<String> targetAddresses) {
    myTargetAddresses = targetAddresses;
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

  public boolean isAnnotationProcessorTarget() {
    return StringUtil.equals("annotation_processor", getInternalPantsTargetType());
  }

  public boolean hasScalaLib() {
    for (String libraryId : libraries) {
      if (StringUtil.startsWith(libraryId, "org.scala-lang:scala-library")) {
        return true;
      }
    }
    return false;
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

  public TargetInfo union(@NotNull TargetInfo other) {
    return new TargetInfo(
      ContainerUtil.union(getLibraries(), other.getLibraries()),
      ContainerUtil.union(getTargets(), other.getTargets()),
      ContainerUtil.union(getRoots(), other.getRoots()),
      getTargetType(),
      ContainerUtil.getLastItem(ContainerUtil.sorted(Arrays.asList(getInternalPantsTargetType(), other.getInternalPantsTargetType()))),
      is_code_gen,
      ContainerUtil.union(getTargetAddresses(), other.getTargetAddresses())
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
           ", targetAddresses=" + myTargetAddresses +
           '}';
  }
}
