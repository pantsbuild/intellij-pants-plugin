// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Map;

public class ProjectInfo {
  public static ProjectInfo fromJson(@NotNull String data) {
    final GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(TargetInfo.class, TargetInfoDeserializer.INSTANCE);
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

  public Map<String, LibraryInfo> getLibraries() {
    return libraries;
  }

  public void setLibraries(Map<String, LibraryInfo> libraries) {
    this.libraries = libraries;
  }

  public Map<String, TargetInfo> getTargets() {
    return targets;
  }

  public void setTargets(Map<String, TargetInfo> targets) {
    this.targets = targets;
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

  public void removeTarget(String targetName) {
    targets.remove(targetName);
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

  @Override
  public String toString() {
    return "ProjectInfo{" +
           "libraries=" + libraries +
           ", targets=" + targets +
           '}';
  }
}
