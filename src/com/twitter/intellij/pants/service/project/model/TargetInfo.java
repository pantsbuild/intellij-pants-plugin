package com.twitter.intellij.pants.service.project.model;

import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.util.PantsSourceType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
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

  public TargetInfo intersect(@NotNull TargetInfo other) {
    final Collection<String> libs = ContainerUtil.intersection(getLibraries(), other.getLibraries());
    final Collection<String> targets = ContainerUtil.intersection(getTargets(), other.getTargets());
    final Collection<SourceRoot> roots = ContainerUtil.intersection(getRoots(), other.getRoots());
    return new TargetInfo(
      new ArrayList<String>(libs),
      new ArrayList<String>(targets),
      new ArrayList<SourceRoot>(roots),
      getTargetType()
    );
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
