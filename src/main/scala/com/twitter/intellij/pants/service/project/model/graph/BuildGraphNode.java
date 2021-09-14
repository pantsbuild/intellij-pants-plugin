// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model.graph;

import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BuildGraphNode and Module are one to one relationship.
 */
public class BuildGraphNode {
  private TargetInfo myTargetInfo;

  public String getAddress() {
    return address;
  }

  private String address; // could be synthetic and tweaked by modifiers.

  public Set<BuildGraphNode> getDependencies() {
    return myDependencies;
  }

  private Set<BuildGraphNode> myDependencies = new HashSet<>();
  private Set<BuildGraphNode> myDependees = new HashSet<>();

  public TargetInfo getTargetInfo() {
    return myTargetInfo;
  }

  public BuildGraphNode(Map.Entry<String, TargetInfo> entry) {
    address = entry.getKey();
    myTargetInfo = entry.getValue();
  }

  public boolean containsTargetAddress(String targetAddress) {
    return myTargetInfo.getAddressInfos().stream().anyMatch(s -> s.getTargetAddress().equals(targetAddress));
  }

  public boolean isTargetRoot() {
    return myTargetInfo.getAddressInfos().stream().anyMatch(TargetAddressInfo::isTargetRoot);
  }

  public boolean isAliasTarget() {
    return myTargetInfo.getAddressInfos().stream().anyMatch(TargetAddressInfo::isTargetAlias);
  }

  public void addDependency(BuildGraphNode node) {
    myDependencies.add(node);
  }

  public void addDepeedee(BuildGraphNode node) {
    myDependees.add(node);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myTargetInfo);
  }

  @Override
  public String toString() {
    return super.toString() + " " +
           String.join(
             " ",
             myTargetInfo.getAddressInfos().stream()
               .map(TargetAddressInfo::getTargetAddress)
               .collect(Collectors.toList())
           );
  }
}
