// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.util.PantsScalaUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
  protected Set<ContentRoot> roots = Collections.emptySet();

  public TargetInfo(TargetAddressInfo... addressInfos) {
    setAddressInfos(ContainerUtil.newHashSet(addressInfos));
  }

  public TargetInfo(
    Set<TargetAddressInfo> addressInfos,
    Set<String> targets,
    Set<String> libraries,
    Set<String> excludes,
    Set<ContentRoot> roots
  ) {
    setAddressInfos(addressInfos);
    setLibraries(libraries);
    setExcludes(excludes);
    setTargets(targets);
    setRoots(roots);
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
    this.libraries = new TreeSet<>(libraries);
  }

  @NotNull
  public Set<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(Set<String> excludes) {
    this.excludes = new TreeSet<>(excludes);
  }

  @NotNull
  public Set<String> getTargets() {
    return targets;
  }

  public void setTargets(Set<String> targets) {
    this.targets = new TreeSet<>(targets);
  }

  @NotNull
  public Set<ContentRoot> getRoots() {
    return roots;
  }

  public void setRoots(Set<ContentRoot> roots) {
    this.roots = new TreeSet<>(roots);
  }

  public boolean isEmpty() {
    return libraries.isEmpty() && targets.isEmpty() && roots.isEmpty() && addressInfos.isEmpty();
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

  public boolean isTest() {
    return ContainerUtil.exists(
      getAddressInfos(),
      new Condition<TargetAddressInfo>() {
        @Override
        public boolean value(TargetAddressInfo info) {
          return PantsUtil.getSourceTypeForTargetType(info.getTargetType(), info.isSynthetic()).toExternalSystemSourceType().isTest();
        }
      }
    );
  }

  @NotNull
  public PantsSourceType getSourcesType() {
    // In the case where multiple targets get combined into one module,
    // the type of common module should be in the order of
    // source -> test source -> resource -> test resources. (like Ranked Value in Pants options)
    // e.g. if source and resources get combined, the common module should be source type.

    Set<PantsSourceType> allTypes = getAddressInfos().stream()
      .map(s -> PantsUtil.getSourceTypeForTargetType(s.getTargetType(), s.isSynthetic()))
      .collect(Collectors.toSet());

    Optional<PantsSourceType> topRankedType = Arrays.stream(PantsSourceType.values())
      .filter(allTypes::contains)
      .findFirst();

    if (topRankedType.isPresent()) {
      return topRankedType.get();
    }
    return PantsSourceType.SOURCE;
  }

  public boolean isJarLibrary() {
    return getAddressInfos().stream().allMatch(TargetAddressInfo::isJarLibrary);
  }

  public boolean isScalaTarget() {
    return getAddressInfos().stream().anyMatch(TargetAddressInfo::isScala) ||
           // TODO(yic): have Pants export `pants_target_type` correctly
           // because `thrift-scala` also has the type `java_thrift_library`
           getAddressInfos().stream().anyMatch(s -> s.getTargetAddress().endsWith("-scala"));
  }

  public boolean isPythonTarget() {
    return getAddressInfos().stream().anyMatch(TargetAddressInfo::isPython);
  }

  public boolean dependOn(@NotNull String targetName) {
    return targets.contains(targetName);
  }

  public void addDependency(@NotNull String targetName) {
    if (targets.isEmpty()) {
      targets = new HashSet<>(Collections.singletonList(targetName));
    }
    else {
      targets.add(targetName);
    }
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
