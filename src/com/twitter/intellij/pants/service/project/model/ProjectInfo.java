// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ProjectInfo {
  public static final String COMMON_SOURCES_TARGET_NAME = "common_sources";
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


  public void fixCommonRoots() {
    // IntelliJ doesn't support when several modules have the same source root
    // so, for source roots that point at multiple targets, we need to convert those so that
    // they have only one target that owns them.
    // to do that, we
    // - find or create a target to own the source root
    // - for each target that depends on that root,
    //   we replace the root with a dependency on the new target

    final Map<SourceRoot, List<Pair<String, TargetInfo>>> sourceRoot2Targets = getSourceRoot2TargetMapping();
    final String pantsWorkingDirPath = findPantsWorkingDirPath(sourceRoot2Targets);

    for (Map.Entry<SourceRoot, List<Pair<String, TargetInfo>>> entry : sourceRoot2Targets.entrySet()) {
      final List<Pair<String, TargetInfo>> targetNameAndInfos = entry.getValue();
      final SourceRoot originalSourceRoot = entry.getKey();
      if (targetNameAndInfos.size() <= 1) {
        continue;
      }

      final Pair<String, TargetInfo> commonTargetNameAndInfo =
        findOrCreateTargetForCommonSourceRoot(pantsWorkingDirPath, targetNameAndInfos, originalSourceRoot);

      addTarget(commonTargetNameAndInfo.getFirst(), commonTargetNameAndInfo.getSecond());
      for (Pair<String, TargetInfo> nameAndInfo : targetNameAndInfos) {
        nameAndInfo.getSecond().getRoots().remove(originalSourceRoot);
        nameAndInfo.getSecond().addDependency(commonTargetNameAndInfo.getFirst());
      }
    }
  }

  @Override
  public String toString() {
    return "ProjectInfo{" +
           "libraries=" + libraries +
           ", targets=" + targets +
           '}';
  }

  @NotNull
  public Map<SourceRoot, List<Pair<String, TargetInfo>>> getSourceRoot2TargetMapping() {
    final Map<SourceRoot, List<Pair<String, TargetInfo>>> result = new HashMap<SourceRoot, List<Pair<String, TargetInfo>>>();
    for (Map.Entry<String, TargetInfo> entry : getTargets().entrySet()) {
      final String targetName = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      for (SourceRoot sourceRoot : targetInfo.getRoots()) {
        ContainerUtil.getOrCreate(result, sourceRoot, new Factory<List<Pair<String, TargetInfo>>>() {
          @Override
          public List<Pair<String, TargetInfo>> create() {
            return new ArrayList<Pair<String, TargetInfo>>();
          }
        }
        ).add(Pair.create(targetName, targetInfo));
      }
    }
    return result;
  }

  @NotNull
  private String findPantsWorkingDirPath(@NotNull Map<SourceRoot, List<Pair<String, TargetInfo>>> sourceRoot2Targets) {
    final Set<Map.Entry<SourceRoot, List<Pair<String, TargetInfo>>>> entries = sourceRoot2Targets.entrySet();
    final String root = entries.iterator().next().getKey().getRawSourceRoot();
    final VirtualFile dir = entries.isEmpty() || StringUtil.isEmpty(root)? null : PantsUtil.findPantsWorkingDir(root);

    return dir != null ? dir.getPath(): "/";
  }

  @NotNull
  private Pair<String, TargetInfo> findOrCreateTargetForCommonSourceRoot(
    @NotNull String path,
    @NotNull List<Pair<String, TargetInfo>> targetNameAndInfos,
    @NotNull SourceRoot originalSourceRoot
  ) {
    Pair<String, TargetInfo> existingTarget = findSingleTargetWithOnlyThisRoot(targetNameAndInfos);
    if (existingTarget != null) {
      return existingTarget;
    }
    TargetInfo commonInfo = createTargetForSourceRootIntersectingDeps(targetNameAndInfos, originalSourceRoot);

    final String commonTargetAddress = createTargetAddressForCommonSource(path, originalSourceRoot);
    return Pair.create(commonTargetAddress, commonInfo);
  }

  @Nullable
  private Pair<String, TargetInfo> findSingleTargetWithOnlyThisRoot(@NotNull List<Pair<String, TargetInfo>> targetNameAndInfos) {
    final List<Pair<String, TargetInfo>> singleRootTargets = ContainerUtil.findAll(
      targetNameAndInfos, new Condition<Pair<String, TargetInfo>>() {
        @Override
        public boolean value(Pair<String, TargetInfo> nameAndInfo) {
          final TargetInfo info = nameAndInfo.getSecond();
          final int commonSourceTargetDependencyCount = ContainerUtil.findAll(
            info.getTargets(), new Condition<String>() {
              @Override
              public boolean value(String s) {
                return s.contains(COMMON_SOURCES_TARGET_NAME);
              }
            }
          ).size();
          return commonSourceTargetDependencyCount + info.getRoots().size() == 1;
        }
      }
    );
    if (singleRootTargets.size() == 1) {
      return singleRootTargets.get(0);
    } else {
      LOG.debug("had more than one target with one source root: " + singleRootTargets);
      return null;
    }
  }

  @NotNull
  private String createTargetAddressForCommonSource(@NotNull String projectPath, @NotNull SourceRoot originalSourceRoot) {
    final String commonPath = originalSourceRoot.getRawSourceRoot();
    String relativePath = commonPath.substring(projectPath.length());
    return relativePath + ":" + COMMON_SOURCES_TARGET_NAME;
  }

  @NotNull
  private TargetInfo createTargetForSourceRootIntersectingDeps(
    @NotNull List<Pair<String, TargetInfo>> targetNameAndInfos,
    @NotNull SourceRoot originalSourceRoot
  ) {
    final Iterator<Pair<String, TargetInfo>> iterator = targetNameAndInfos.iterator();
    TargetInfo commonInfo = iterator.next().getSecond();
    while (iterator.hasNext()) {
      commonInfo = commonInfo.intersect(iterator.next().getSecond());
    }
    final Set<SourceRoot> newRoots = ContainerUtil.newHashSet(originalSourceRoot);
    commonInfo.setRoots(newRoots);
    return commonInfo;
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
}
