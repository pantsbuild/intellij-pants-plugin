// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model.graph;


import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.service.project.model.TargetInfo;

import java.util.ArrayDeque;
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
  private Set<BuildGraphNode> allNodes = new HashSet<>();

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
        .map(BuildGraphNode::new)
        .collect(Collectors.toList())
    );

    // then process their relationships, dependencies and dependees
    for (BuildGraphNode node : allNodes) {
      Set<String> deps = node.getTargetInfo().getTargets();
      for (String dep : deps) {
        // FIXME: getNode is currently linear search.
        Optional<BuildGraphNode> depNode = getNode(dep);
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
    Set<BuildGraphNode> currentNodeSet = getTargetRoots();

    while (true) {
      Set<BuildGraphNode> dependencyNodes = currentNodeSet.stream()
        .map(BuildGraphNode::getDependencies)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());

      int sizeBeforeAdd = currentNodeSet.size();
      currentNodeSet.addAll(dependencyNodes);
      if (currentNodeSet.size() == sizeBeforeAdd) {
        if (currentNodeSet.size() == allNodes.size()) {
          return depth;
        }
        else {
          Set<BuildGraphNode> orphanNodes = Sets.difference(allNodes, currentNodeSet);
          throw new OrphanedNodeException(String.format(ERROR_ORPHANED_NODE, orphanNodes));
        }
      }
      depth++;
    }
  }

  private Set<BuildGraphNode> expandAliasTargets(Set<BuildGraphNode> initialTargets) {
    Set<BuildGraphNode> results = Sets.newHashSet();
    ArrayDeque<BuildGraphNode> q = Queues.newArrayDeque(initialTargets);
    while (!q.isEmpty()) {
      BuildGraphNode curr = q.pop();
      if (curr.isAliasTarget()) {
        q.addAll(curr.getDependencies());
      }
      else {
        results.add(curr);
      }
    }
    return results;
  }

  // level 0 - target roots
  // level 1 - target roots + direct deps
  // ...
  public Set<BuildGraphNode> getNodesUpToLevel(int level) {
    // Holds the current scope of build graph.
    Set<BuildGraphNode> results = getTargetRoots();
    results.addAll(expandAliasTargets(results));

    for (int i = 0; i < level; i++) {
      Set<BuildGraphNode> dependencies = new HashSet<>();
      for (BuildGraphNode node : results) {
        dependencies.addAll(node.getDependencies());
      }
      results.addAll(dependencies);
      results.addAll(expandAliasTargets(dependencies));
      // All nodes are in, no need to iterate more.
      if (results.size() == allNodes.size()) {
        break;
      }
    }
    return results;
  }

  private Set<BuildGraphNode> getTargetRoots() {
    Set<BuildGraphNode> targetRoots = allNodes.stream().filter(BuildGraphNode::isTargetRoot).collect(Collectors.toSet());
    if (targetRoots.isEmpty()) {
      throw new NoTargetRootException(ERROR_NO_TARGET_ROOT);
    }
    return targetRoots;
  }

  private Optional<BuildGraphNode> getNode(String targetAddress) {
    return allNodes.stream()
      .filter(n -> n.getAddress().equals(targetAddress))
      .findFirst();
  }
}
