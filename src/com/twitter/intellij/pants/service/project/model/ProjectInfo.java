package com.twitter.intellij.pants.service.project.model;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  public Map<String, String> combinedTargets = new HashMap<String, String>();

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

  @Override
  public String toString() {
    return "ProjectInfo{" +
           "libraries=" + libraries +
           ", targets=" + targets +
           '}';
  }

  public TargetInfo getTarget(String targetName) {
    if (combinedTargets.containsKey(targetName)) {
      return this.targets.get(combinedTargets.get(targetName));
    }
    return this.targets.get(targetName);
  }

  public void setCombinedScalaJavaTargets(Map<String, String> combinedTargets) {
    this.combinedTargets = combinedTargets;
  }
}
