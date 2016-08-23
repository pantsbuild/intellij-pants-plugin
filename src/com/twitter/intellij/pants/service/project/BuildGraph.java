// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;


import com.intellij.openapi.diagnostic.Logger;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class BuildGraph {
  private Logger logger = Logger.getInstance("#" + BuildGraph.class.getName());
  private Set<Node> allNodes = new HashSet<>();

  public BuildGraph(ProjectInfo projectInfo) {
    // add everybody in
    for (TargetInfo targetinfo : projectInfo.getTargets().values()) {
      allNodes.add(new Node(targetinfo));
    }

    // then process their relationships, dependencies and dependees
    for (Node node : allNodes) {
      Set<String> deps = node.getTargetInfo().getTargets();
      for (String dep : deps) {
        Node depNode = getNode(dep);
        if (depNode != null) {
          node.addDependency(depNode);
          depNode.addDepeedee(node);
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

  @Nullable
  private Node getNode(String targetAddress) {
    return allNodes.stream()
      .filter(n -> n.containsTargetAddress(targetAddress)).findFirst().orElse(null);
  }

  public class Node {
    private TargetInfo myTargetInfo;

    public Set<Node> getDependencies() {
      return myDependencies;
    }

    private Set<Node> myDependencies = new HashSet<>();
    private Set<Node> myDependees = new HashSet<>();

    public TargetInfo getTargetInfo() {
      return myTargetInfo;
    }

    public Node(TargetInfo targetInfo) {
      myTargetInfo = targetInfo;
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
