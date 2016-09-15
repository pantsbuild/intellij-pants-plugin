// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;


import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.model.TargetAddressInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BuildGraph {
  public static final String ERROR_ORPHANED_NODE = "Missing link in build graph. Orphan nodes: %s";
  public static final String ERROR_NO_TARGET_ROOT =
    "No target roots found in build graph. Please make sure Pants export version >= 1.0.9";

  private static Logger logger = Logger.getInstance("#" + BuildGraph.class.getName());
  private Set<Node> allNodes = new HashSet<>();

  public class OrphanedNodeException extends PantsException {

    public OrphanedNodeException(String message) {
      super(message);
    }
  }

  public class NoTargetRootException extends PantsException {

    public NoTargetRootException(String message) {
      super(message);
    }
  }

  public BuildGraph(ProjectInfo projectInfo) {
    // add everybody in
    Map<String, TargetInfo> targets = projectInfo
      .getTargets();
    processTargets(targets);
  }

  public BuildGraph(Map<String, TargetInfo> targets) {
    processTargets(targets);
  }

  private void processTargets(Map<String, TargetInfo> targets) {
    allNodes.addAll(
      targets
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
    int lastNodeCount = -1;

    while (true) {
      Set<Node> nodesByLevel = getNodesByLevel(depth);
      if (nodesByLevel.size() == lastNodeCount && nodesByLevel.size() != allNodes.size()) {
        Set<Node> orphanNodes = Sets.difference(allNodes, nodesByLevel);
        throw new OrphanedNodeException(String.format(ERROR_ORPHANED_NODE, orphanNodes));
      }

      if (nodesByLevel.size() == allNodes.size()) {
        return depth;
      }
      lastNodeCount = nodesByLevel.size();
      depth++;
    }
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
    Set<Node> targetRoots = allNodes.stream().filter(Node::isTargetRoot).collect(Collectors.toSet());
    if (targetRoots.isEmpty()) {
      throw new NoTargetRootException(ERROR_NO_TARGET_ROOT);
    }
    return targetRoots;
  }

  private Optional<Node> getNode(String targetAddress) {
    return allNodes.stream()
      .filter(n -> n.getAddress().equals(targetAddress))
      .findFirst();
  }

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
}
