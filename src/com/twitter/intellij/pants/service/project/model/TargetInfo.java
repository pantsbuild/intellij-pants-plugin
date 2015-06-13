// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.util.PantsScalaUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class TargetInfo {

  protected Set<TargetAddressInfo> addressInfos = Collections.emptySet();

  /**
   * List of libraries. Just names.
   */
  protected Set<String> libraries = Collections.emptySet();
  /**
   * List of libraries. Just names.
   */
  protected Set<String> excludes = Collections.emptySet();
  /**
   * List of dependencies.
   */
  protected Set<String> targets = Collections.emptySet();
  /**
   * List of source roots.
   */
  protected Set<SourceRoot> roots = Collections.emptySet();

  public TargetInfo(
    Set<TargetAddressInfo> addressInfos,
    Set<String> targets,
    Set<String> libraries,
    Set<String> excludes,
    Set<SourceRoot> roots
  ) {
    this.addressInfos = addressInfos;
    this.libraries = libraries;
    this.excludes = excludes;
    this.targets = targets;
    this.roots = roots;
  }

  public Set<TargetAddressInfo> getAddressInfos() {
    return addressInfos;
  }

  public void setAddressInfos(Set<TargetAddressInfo> addressInfos) {
    this.addressInfos = addressInfos;
  }

  @NotNull
  public Set<String> getLibraries() {
    return libraries;
  }

  public void setLibraries(Set<String> libraries) {
    this.libraries = libraries;
  }

  @NotNull
  public Set<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(Set<String> excludes) {
    this.excludes = excludes;
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

  public boolean isEmpty() {
    return libraries.isEmpty() && targets.isEmpty() && roots.isEmpty();
  }

  @Nullable
  public String findScalaLibId() {
    return ContainerUtil.find(
      libraries,
      new Condition<String>() {
        @Override
        public boolean value(String libraryId) {
          return PantsScalaUtil.isScalaLibraryLib(libraryId);
        }
      }
    );
  }

  @NotNull
  public PantsSourceType getSourcesType() {
    // todo: take it smarter
    final Iterator<TargetAddressInfo> iterator = getAddressInfos().iterator();
    return iterator.hasNext() ? PantsUtil.getSourceTypeForTargetType(iterator.next().getTargetType()) : PantsSourceType.SOURCE;
  }

  /**
   * @return true if not an actual target
   */
  public boolean isDummy() {
    return addressInfos.isEmpty();
  }

  public boolean isJarLibrary() {
    return PantsUtil.forall(
      getAddressInfos(),
      new Condition<TargetAddressInfo>() {
        @Override
        public boolean value(TargetAddressInfo info) {
          return info.isJarLibrary();
        }
      }
    );
  }

  public boolean isScalaTarget() {
    return ContainerUtil.exists(
      getAddressInfos(),
      new Condition<TargetAddressInfo>() {
        @Override
        public boolean value(TargetAddressInfo info) {
          return info.isScala();
        }
      }
    );
  }

  public boolean dependOn(@NotNull String targetName) {
    return targets.contains(targetName);
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
      ContainerUtil.union(getAddressInfos(), other.getAddressInfos()),
      ContainerUtil.union(getTargets(), other.getTargets()),
      ContainerUtil.union(getLibraries(), other.getLibraries()),
      ContainerUtil.union(getExcludes(), other.getExcludes()),
      ContainerUtil.union(getRoots(), other.getRoots())
    );
  }

  @Override
  public String toString() {
    return "TargetInfo{" +
           "libraries=" + libraries +
           ", excludes=" + excludes +
           ", targets=" + targets +
           ", roots=" + roots +
           ", addressInfos='" + addressInfos + '\'' +
           '}';
  }
}
