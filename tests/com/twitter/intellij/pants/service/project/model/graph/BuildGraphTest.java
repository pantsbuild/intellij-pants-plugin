// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model.graph;

import com.google.common.collect.Sets;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfoTest;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BuildGraphTest extends TestCase {

  private static final boolean IS_TARGET_ROOT = true;
  private Map<String, TargetInfo> targets;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    targets = new HashMap<>();
  }

  public void test1() throws Exception {
    injectTargetInfo(targets, "a", "source", IS_TARGET_ROOT, Optional.empty());
    assertEquals(0, new BuildGraph(targets).getMaxDepth());
  }

  public void test2() throws Exception {
    injectTargetInfo(targets, "a", "source", IS_TARGET_ROOT, Optional.empty());
    injectTargetInfo(targets, "b", "source", IS_TARGET_ROOT, Optional.of("a"));
    // a (root) -> b (root), so level is 0
    assertEquals(0, new BuildGraph(targets).getMaxDepth());
  }

  public void test3() throws Exception {
    injectTargetInfo(targets, "a", "source", IS_TARGET_ROOT, Optional.empty());
    injectTargetInfo(targets, "b", "source", !IS_TARGET_ROOT, Optional.of("a"));
    // a (root) -> b, level 1
    assertEquals(1, new BuildGraph(targets).getMaxDepth());

    injectTargetInfo(targets, "c", "source", !IS_TARGET_ROOT, Optional.of("b"));
    // a (root) -> b -> c, level 2
    BuildGraph graph = new BuildGraph(targets);
    assertEquals(2, graph.getMaxDepth());

    assertEquals(
      Sets.newHashSet("a"),
      getAddressesByLevel(graph, 0)
    );
    assertEquals(
      Sets.newHashSet("a", "b"),
      getAddressesByLevel(graph, 1)
    );
    assertEquals(
      Sets.newHashSet("a", "b", "c"),
      getAddressesByLevel(graph, 2)
    );
  }

  private Set<String> getAddressesByLevel(BuildGraph graph, int level) {
    return graph.getNodesUpToLevel(level).stream().map(BuildGraphNode::getAddress).collect(Collectors.toSet());
  }

  public void test4() throws Exception {
    injectTargetInfo(targets, "a", "source", IS_TARGET_ROOT, Optional.empty());
    injectTargetInfo(targets, "b", "source", IS_TARGET_ROOT, Optional.of("a"));
    injectTargetInfo(targets, "c", "source", !IS_TARGET_ROOT, Optional.of("b"));
    // a (root) -> (root) b -> c, level 1
    BuildGraph graph = new BuildGraph(targets);
    assertEquals(1, graph.getMaxDepth());
    assertEquals(
      Sets.newHashSet("a", "b"),
      getAddressesByLevel(graph, 0)
    );
  }

  public void testNoTargetRoot() throws Exception {
    injectTargetInfo(targets, "a", "source", !IS_TARGET_ROOT, Optional.empty());
    BuildGraph graph = new BuildGraph(targets);
    try {
      graph.getMaxDepth();
      fail(String.format("Should fail because with %s, but passed.", BuildGraph.NoTargetRootException.class));
    }
    catch (BuildGraph.NoTargetRootException e) {

    }
  }

  public void testOrphan() throws Exception {
    injectTargetInfo(targets, "a", "source", IS_TARGET_ROOT, Optional.empty());
    injectTargetInfo(targets, "b", "source", !IS_TARGET_ROOT, Optional.empty());
    // a (root), b, so b is orphaned.
    try {
      assertEquals(1, new BuildGraph(targets).getMaxDepth());
      fail(String.format("Should fail with %s, but passed.", BuildGraph.OrphanedNodeException.class));
    }
    catch (BuildGraph.OrphanedNodeException e) {

    }
  }

  public void testTargetAliasExpansion1() {
    // a -> b
    injectTargetInfoWithInternalPantsTargetType(targets, "a", "source", "target", IS_TARGET_ROOT, Optional.empty());
    injectTargetInfoWithInternalPantsTargetType(targets, "b", "source", "java_library", !IS_TARGET_ROOT, Optional.of("a"));
    // a (root) -> b, level 1
    assertEquals(1, new BuildGraph(targets).getMaxDepth());
    // because 'a' is an alias, so its deps will automatically get expanded if 'a' is included.
    assertEquals(2, new BuildGraph(targets).getNodesUpToLevel(0).size());
  }

  public void testTargetAliasExpansion2() {
    // a -> b
    injectTargetInfoWithInternalPantsTargetType(targets, "a", "source", "alias", IS_TARGET_ROOT, Optional.empty());
    injectTargetInfoWithInternalPantsTargetType(targets, "b", "source", "java_library", !IS_TARGET_ROOT, Optional.of("a"));
    // a (root) -> b, level 1
    assertEquals(1, new BuildGraph(targets).getMaxDepth());
    // because 'a' is an alias, so its deps will automatically get expanded if 'a' is included.
    assertEquals(2, new BuildGraph(targets).getNodesUpToLevel(0).size());
  }

  public void testTargetAliasExpansion3() {
    // a -> b
    injectTargetInfoWithInternalPantsTargetType(targets, "a", "source", "java_library", IS_TARGET_ROOT, Optional.empty());
    injectTargetInfoWithInternalPantsTargetType(targets, "b", "source", "target", !IS_TARGET_ROOT, Optional.of("a"));
    // a (root) -> b, level 1
    assertEquals(1, new BuildGraph(targets).getMaxDepth());
    assertEquals(1, new BuildGraph(targets).getNodesUpToLevel(0).size());
  }

  public void testTargetAliasExpansion4() {
    // a -> b -> c
    //      b -> d
    injectTargetInfoWithInternalPantsTargetType(targets, "a", "source", "java_library", IS_TARGET_ROOT, Optional.empty());
    injectTargetInfoWithInternalPantsTargetType(targets, "b", "source", "target", !IS_TARGET_ROOT, Optional.of("a"));
    injectTargetInfoWithInternalPantsTargetType(targets, "c", "source", "java_library", !IS_TARGET_ROOT, Optional.of("b"));
    injectTargetInfoWithInternalPantsTargetType(targets, "d", "source", "java_library", !IS_TARGET_ROOT, Optional.of("b"));
    // a (root) -> b, level 1
    assertEquals(2, new BuildGraph(targets).getMaxDepth());
    // depth 0 only has target 'a'
    assertEquals(1, new BuildGraph(targets).getNodesUpToLevel(0).size());
    // depth 1 initially has 'a' & 'b', but because 'b' is an alias, its deps 'c' & 'd' will be added'
    assertEquals(4, new BuildGraph(targets).getNodesUpToLevel(1).size());
  }

  private void injectTargetInfoWithInternalPantsTargetType(
    Map<String, TargetInfo> targets,
    String targetAddress,
    String targetType,
    String internalPantsTargetType,
    boolean is_target_root,
    Optional<String> dependee
  ) {
    TargetInfo info = injectTargetInfo(targets, targetAddress, targetType, is_target_root, dependee);
    info.getAddressInfos().stream().forEach(s -> s.setPantsTargetType(internalPantsTargetType));
  }

  private TargetInfo injectTargetInfo(
    Map<String, TargetInfo> targets,
    String targetAddress,
    String targetType,
    boolean is_target_root,
    Optional<String> dependee
  ) {
    TargetInfo source = TargetInfoTest.createTargetInfoWithTargetAddressInfo(targetType);
    source.getAddressInfos().forEach(s -> s.setIsTargetRoot(is_target_root));

    //getTarget here is actually getting dependencies :/
    if (dependee.isPresent()) {
      targets.entrySet().forEach(s -> {
        if (s.getKey().equals(dependee.get())) {
          s.getValue().addDependency(targetAddress);
        }
      });
    }

    targets.put(targetAddress, source);
    return source;
  }
}
