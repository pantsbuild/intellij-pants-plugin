// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ProjectInfo {
  private final Logger LOG = Logger.getInstance(getClass());
  // id(org:name:version) to jars
  protected Map<String, List<String>> libraries;
  // name to info
  protected Map<String, TargetInfo> targets;

  public Map<String, List<String>> getLibraries() {
    return libraries;
  }

  public void setLibraries(Map<String, List<String>> libraries) {
    this.libraries = libraries;
  }

  public Map<String, TargetInfo> getTargets() {
    return targets;
  }

  public void setTargets(Map<String, TargetInfo> targets) {
    this.targets = targets;
  }

  public List<String> getLibraries(@NotNull String libraryId) {
    if (libraries.containsKey(libraryId) && libraries.get(libraryId).size() > 0) {
      return libraries.get(libraryId);
    }
    int versionIndex = libraryId.lastIndexOf(':');
    if (versionIndex == -1) {
      return Collections.emptyList();
    }
    final String libraryName = libraryId.substring(0, versionIndex);
    for (Map.Entry<String, List<String>> libIdAndJars : libraries.entrySet()) {
      final String currentLibraryId = libIdAndJars.getKey();
      if (!StringUtil.startsWith(currentLibraryId, libraryName + ":")) {
        continue;
      }
      final List<String> currentJars = libIdAndJars.getValue();
      if (!currentJars.isEmpty()) {
        LOG.info("Using " + currentLibraryId + " instead of " + libraryId);
        return currentJars;
      }
    }
    return Collections.emptyList();
  }

  public TargetInfo getTarget(String targetName) {
    return targets.get(targetName);
  }

  public void addTarget(String targetName, TargetInfo info) {
    targets.put(targetName, info);
  }

  public void removeTarget(String targetName) {
    targets.remove(targetName);
  }

  public void replaceDependency(String targetName, String newTargetName) {
    for (TargetInfo targetInfo : targets.values()) {
      targetInfo.replaceDependency(targetName, newTargetName);
    }
  }

  public void fixCyclicDependencies() {
    final Set<Map.Entry<String, TargetInfo>> originalEntries =
      new HashSet<Map.Entry<String, TargetInfo>>(getTargets().entrySet());
    for (Map.Entry<String, TargetInfo> nameAndInfo : originalEntries) {
      final String targetName = nameAndInfo.getKey();
      final TargetInfo targetInfo = nameAndInfo.getValue();
      if (!getTargets().containsKey(targetName)) {
        // already removed
        continue;
      }
      for (String dependencyTargetName : targetInfo.getTargets()) {
        TargetInfo dependencyTargetInfo = getTarget(dependencyTargetName);
        if (dependencyTargetInfo != null && dependencyTargetInfo.dependOn(targetName)) {
          LOG.info(String.format("Found cyclic dependency between %s and %s", targetName, dependencyTargetName));

          final String combinedTargetName = combinedTargetsName(targetName, dependencyTargetName);
          final TargetInfo combinedInfo = targetInfo.union(dependencyTargetInfo);
          combinedInfo.removeDependency(targetName);
          combinedInfo.removeDependency(dependencyTargetName);
          addTarget(combinedTargetName, combinedInfo);

          replaceDependency(targetName, combinedTargetName);
          removeTarget(targetName);

          replaceDependency(dependencyTargetName, combinedTargetName);
          removeTarget(dependencyTargetName);
        }
      }
    }
  }

  @NotNull
  private String combinedTargetsName(String... targetNames) {
    assert targetNames.length > 0;
    String commonPrefix = targetNames[0];
    for (String name : targetNames) {
      commonPrefix = StringUtil.commonPrefix(commonPrefix, name);
    }
    final String finalCommonPrefix = commonPrefix;
    return
      commonPrefix +
      StringUtil.join(
        ContainerUtil.sorted(
          ContainerUtil.map(
            targetNames,
            new Function<String, String>() {
              @Override
              public String fun(String targetName) {
                return targetName.substring(finalCommonPrefix.length());
              }
            }
          )
        ),
        "_and_"
      );
  }

  @Override
  public String toString() {
    return "ProjectInfo{" +
           "libraries=" + libraries +
           ", targets=" + targets +
           '}';
  }

  public Map<SourceRoot, List<Pair<String, TargetInfo>>> getSourceRoot2TargetMapping() {
    final Factory<List<Pair<String, TargetInfo>>> factory = new Factory<List<Pair<String, TargetInfo>>>() {
      @Override
      public List<Pair<String, TargetInfo>> create() {
        return new ArrayList<Pair<String, TargetInfo>>();
      }
    };
    final HashMap<SourceRoot, List<Pair<String, TargetInfo>>> result = new HashMap<SourceRoot, List<Pair<String, TargetInfo>>>();
    for (Map.Entry<String, TargetInfo> entry : getTargets().entrySet()) {
      final String targetName = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      for (SourceRoot sourceRoot : targetInfo.getRoots()) {
        ContainerUtil.getOrCreate(result, sourceRoot, factory).add(Pair.create(targetName, targetInfo));
      }
    }
    return result;
  }
}
