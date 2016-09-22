// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model.graph;


import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.service.project.model.TargetInfo;

import java.util.HashSet;
import java.util.Map;
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

  public BuildGraph(Map<String, TargetInfo> targets) {
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
        // FIXME: getNode is currently linear search.
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

  // FIXME: not the most efficient way to find max depth yet.
  public int getMaxDepth() {
    int depth = 0;
    Set<Node> results = getTargetRoots();

    while (true) {
      Set<Node> dependencies = new HashSet<>();
      for (Node node : results) {
        dependencies.addAll(node.getDependencies());
      }
      int resultBefore = results.size();
      results.addAll(dependencies);
      if (results.size() == resultBefore) {
        if (results.size() == allNodes.size()){
          return depth;
        }
        else {
          Set<Node> orphanNodes = Sets.difference(allNodes, results);
          throw new OrphanedNodeException(String.format(ERROR_ORPHANED_NODE, orphanNodes));
        }
      }
      depth++;
    }
  }

  // level 0 - target roots
  // level 1 - target roots + direct deps
  // ...
  public Set<Node> getNodesUpToLevel(int level) {
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
}
