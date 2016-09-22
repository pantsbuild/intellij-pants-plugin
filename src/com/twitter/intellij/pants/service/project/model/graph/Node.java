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
 * Node and Module are one to one relationship.
 */
public class Node {
  private TargetInfo myTargetInfo;

  public String getAddress() {
    return address;
  }

  private String address; // could be synthetic and tweaked by modifiers.

  public Set<Node> getDependencies() {
    return myDependencies;
  }

  private Set<Node> myDependencies = new HashSet<>();
  private Set<Node> myDependees = new HashSet<>();

  public TargetInfo getTargetInfo() {
    return myTargetInfo;
  }

  public Node(Map.Entry<String, TargetInfo> entrySet) {
    address = entrySet.getKey();
    myTargetInfo = entrySet.getValue();
  }

  public boolean containsTargetAddress(String targetAddress) {
    return myTargetInfo.getAddressInfos().stream().anyMatch(s -> s.getTargetAddress().equals(targetAddress));
  }

  public boolean isTargetRoot() {
    return myTargetInfo.getAddressInfos().stream().anyMatch(TargetAddressInfo::isTargetRoot);
  }

  public void addDependency(Node node) {
    myDependencies.add(node);
  }

  public void addDepeedee(Node node) {
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
