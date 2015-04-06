// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProjectInfo {
  public static ProjectInfo fromJson(@NotNull String data) {
    final ProjectInfo projectInfo = new Gson().fromJson(data, ProjectInfo.class);
    projectInfo.initTargetAddresses();
    return projectInfo;
  }

  @TestOnly
  public ProjectInfo() {
  }

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

  private void initTargetAddresses() {
    for (Map.Entry<String, TargetInfo> entry : targets.entrySet()) {
      final TargetInfo info = entry.getValue();
      final String address = entry.getKey();
      info.setTargetAddresses(ContainerUtil.set(address));
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
