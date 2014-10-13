package com.twitter.intellij.pants.service.project.model;

import java.util.List;

public class TargetInfo {
  public TargetInfo() {
  }

  public TargetInfo(
    List<String> libraries,
    List<String> targets,
    List<SourceRoot> roots,
    String target_type
  ) {
    this.libraries = libraries;
    this.targets = targets;
    this.roots = roots;
    this.target_type = target_type;
  }

  /**
   * List of libraries. Just names.
   */
  protected List<String> libraries;
  /**
   * List of dependencies.
   */
  protected List<String> targets;
  /**
   * List of source roots.
   */
  protected List<SourceRoot> roots;
  /**
   * Target type.
   */
  protected String target_type;

  public List<String> getLibraries() {
    return libraries;
  }

  public void setLibraries(List<String> libraries) {
    this.libraries = libraries;
  }

  public List<String> getTargets() {
    return targets;
  }

  public void setTargets(List<String> targets) {
    this.targets = targets;
  }

  public List<SourceRoot> getRoots() {
    return roots;
  }

  public void setRoots(List<SourceRoot> roots) {
    this.roots = roots;
  }

  public String getTargetType() {
    return target_type;
  }

  public void setTargetType(String target_type) {
    this.target_type = target_type;
  }

  public boolean isEmpty() {
    return libraries.isEmpty() && targets.isEmpty() && roots.isEmpty();
  }

  @Override
  public String toString() {
    return "TargetInfo{" +
           "libraries=" + libraries +
           ", targets=" + targets +
           ", roots=" + roots +
           ", target_type='" + target_type + '\'' +
           '}';
  }
}
