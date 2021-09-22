// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectInfo {
  public static ProjectInfo fromJson(@NotNull String data) {
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(TargetInfo.class, TargetInfoDeserializer.INSTANCE);
    builder.registerTypeAdapter(LibraryInfo.class, LibraryInfoDeserializer.INSTANCE);
    final ProjectInfo projectInfo = builder.create().fromJson(data, ProjectInfo.class);
    projectInfo.initTargetAddresses();

    return projectInfo;
  }

  @TestOnly
  public ProjectInfo() {
  }

  private final Logger LOG = Logger.getInstance(getClass());
  // id(org:name:version) to jars
  protected Map<String, LibraryInfo> libraries;
  // name to info
  protected Map<String, TargetInfo> targets;

  /* This might need to be expanded to show all properties that
   * a target type can contain like:
   *
   * {"java_library" : [{ "dependencies" : "list(str)" }]}
   */
  @SerializedName("available_target_types")
  protected String[] availableTargetTypes = {};

  protected String version;

  @NotNull
  public String getVersion() {
    return version;
  }

  @Nullable
  protected PythonSetup python_setup = null;

  private static <T> List<Map.Entry<String, T>> getSortedEntries(Map<String, T> map) {
    return ContainerUtil.sorted(
      map.entrySet(),
      new Comparator<Map.Entry<String, T>>() {
        @Override
        public int compare(Map.Entry<String, T> o1, Map.Entry<String, T> o2) {
          return StringUtil.naturalCompare(o1.getKey(), o2.getKey());
        }
      }
    );
  }

  public List<Map.Entry<String, LibraryInfo>> getSortedLibraries() {
    return getSortedEntries(getLibraries());
  }

  public Map<String, LibraryInfo> getLibraries() {
    return Collections.unmodifiableMap(libraries);
  }

  public void setLibraries(Map<String, LibraryInfo> libraries) {
    this.libraries = libraries;
  }

  public List<Map.Entry<String, TargetInfo>> getSortedTargets() {
    return getSortedEntries(getTargets());
  }

  public Map<String, TargetInfo> getTargets() {
    return Collections.unmodifiableMap(targets);
  }

  public void setTargets(Map<String, TargetInfo> targets) {
    this.targets = targets;
  }

  @NotNull
  public String[] getAvailableTargetTypes() {
    return availableTargetTypes;
  }

  @Nullable
  public PythonSetup getPythonSetup() {
    return python_setup;
  }

  @Nullable
  public LibraryInfo getLibraries(@NotNull String libraryId) {
    if (libraries.containsKey(libraryId) && libraries.get(libraryId).getDefault() != null) {
      return libraries.get(libraryId);
    }
    int versionIndex = libraryId.lastIndexOf(':');
    if (versionIndex == -1) {
      return null;
    }
    final String libraryName = libraryId.substring(0, versionIndex);
    for (Map.Entry<String, LibraryInfo> libIdAndJars : libraries.entrySet()) {
      final String currentLibraryId = libIdAndJars.getKey();
      if (!StringUtil.startsWith(currentLibraryId, libraryName + ":")) {
        continue;
      }
      final LibraryInfo currentInfo = libIdAndJars.getValue();
      if (currentInfo != null) {
        LOG.info("Using " + currentLibraryId + " instead of " + libraryId);
        return currentInfo;
      }
    }
    return null;
  }

  @Nullable
  public TargetInfo getTarget(String targetName) {
    return targets.get(targetName);
  }

  public void addTarget(String targetName, TargetInfo info) {
    targets.put(targetName, info);
  }

  public void removeTargets(Collection<String> targetNames) {
    for (String targetName : targetNames) {
      removeTarget(targetName);
    }
  }

  public void renameTarget(@NotNull String targetName, @NotNull String newTargetName) {
    addTarget(newTargetName, getTarget(targetName));
    replaceDependency(targetName, newTargetName);
    removeTarget(targetName);
  }

  public void removeTarget(String targetName) {
    targets.remove(targetName);
    for (TargetInfo targetInfo : targets.values()) {
      targetInfo.getTargets().remove(targetName);
    }
  }

  public void replaceDependency(String targetName, String newTargetName) {
    for (TargetInfo targetInfo : targets.values()) {
      targetInfo.replaceDependency(targetName, newTargetName);
    }
  }

  private void initTargetAddresses() {
    for (Map.Entry<String, TargetInfo> entry : targets.entrySet()) {
      final TargetInfo info = entry.getValue();
      final String address = entry.getKey();
      for (TargetAddressInfo addressInfo : info.getAddressInfos()) {
        addressInfo.setTargetAddress(address);
      }
    }
  }

  /**
   * Helper method to get a distribution by target type.
   */
  public Map<String, Integer> getTargetsDistribution() {
    final Map<String, Integer> result = new HashMap<>();
    for (TargetInfo targetInfo : targets.values()) {
      for (TargetAddressInfo addressInfo : targetInfo.getAddressInfos()) {
        final String type = addressInfo.getInternalPantsTargetType();
        final int currentValue = ContainerUtil.getOrCreate(result, type, 0);
        result.put(type, currentValue + 1);
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return "ProjectInfo{" +
           "libraries=" + libraries +
           ", targets=" + targets +
           '}';
  }
}
