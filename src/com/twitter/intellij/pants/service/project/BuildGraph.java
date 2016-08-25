// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;


import com.intellij.openapi.diagnostic.Logger;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BuildGraph {
  private static Logger logger = Logger.getInstance("#" + BuildGraph.class.getName());
  private Set<Node> allNodes = new HashSet<>();

  public BuildGraph(ProjectInfo projectInfo) {
    // add everybody in
    allNodes.addAll(
      projectInfo
        .getTargets()
        .entrySet()
        .stream()
        .map(Node::new)
        .collect(Collectors.toList())
    );

    // then process their relationships, dependencies and dependees
    for (Node node : allNodes) {
      Set<String> deps = node.getTargetInfo().getTargets();
      for (String dep : deps) {
        // getNode is currently linear search.
        Optional<Node> depNode = getNode(dep);
        if (depNode.isPresent()) {
          node.addDependency(depNode.get());
          depNode.get().addDepeedee(node);
        }
        else {
          logger.error(String.format("No build graph node found for %s", dep));
        }
      }
    }
  }

  public int getMaxDepth() {
    int depth = 0;
    while (getNodesByLevel(depth).size() != allNodes.size()) {
      depth++;
    }
    return depth;
  }

  // level 0 - target roots
  // level 1 - direct deps
  // ...
  public Set<Node> getNodesByLevel(int level) {
    // Holds the current scope of build graph.
    Set<Node> results = getTargetRoots();
    for (int i = 0; i < level; i++) {
      Set<Node> dependencies = new HashSet<>();
      for (Node node : results) {
        dependencies.addAll(node.getDependencies());
      }
      results.addAll(dependencies);
      // All nodes are in, no need to iterate more.
      if (results.size() == allNodes.size()) {
        break;
      }
    }
    return results;
  }

  private Set<Node> getTargetRoots() {
    return allNodes.stream().filter(Node::isTargetRoot).collect(Collectors.toSet());
  }

  private Optional<Node> getNode(String targetAddress) {
    return allNodes.stream()
      .filter(n -> n.getAddress().equals(targetAddress))
      .findFirst();
  }

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
      return super.toString() +
             " " + String.join(
        " ",
        myTargetInfo.getAddressInfos().stream().map(TargetAddressInfo::getTargetAddress).collect(
          Collectors.toList())
      );
    }
  }
}
