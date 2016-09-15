// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.intellij.util.containers.HashMap;
import com.twitter.intellij.pants.PantsException;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public class BuildGraphTest extends TestCase {

  private static final boolean IS_TARGET_ROOT = true;
  private Map<String, TargetInfo> targets;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    targets = new HashMap<>();
  }

  public void testOneRoot() throws Exception {
    injectTargetInfo(targets, "a", "source", IS_TARGET_ROOT, Optional.empty());
    assertEquals(0, new BuildGraph(targets).getMaxDepth());
  }

  public void testTwoRoots() throws Exception {
    injectTargetInfo(targets, "a", "source", IS_TARGET_ROOT, Optional.empty());
    injectTargetInfo(targets, "b", "source", IS_TARGET_ROOT, Optional.of("a"));
    // a, b are both roots, so level is 0
    assertEquals(0, new BuildGraph(targets).getMaxDepth());
  }

  public void testOneROot() throws Exception {
    injectTargetInfo(targets, "a", "source", IS_TARGET_ROOT, Optional.empty());
    injectTargetInfo(targets, "b", "source", !IS_TARGET_ROOT, Optional.of("a"));
    // a -> b, level 1
    assertEquals(1, new BuildGraph(targets).getMaxDepth());
  }

  public void testNoTargetRoot() throws Exception {
    TargetInfo source = TargetInfoTest.createTargetInfoWithTargetAddressInfo("source");
    Map<String, TargetInfo> targets = new HashMap<>();
    targets.put("a", source);

    BuildGraph graph = new BuildGraph(targets);
    try {
      graph.getMaxDepth();
      fail("Should fail because of no target root, but passed.");
    }
    catch (PantsException e) {

    }
  }

  private void injectTargetInfo(
    Map<String, TargetInfo> targets,
    String targetAddress,
    String type,
    boolean is_target_root,
    Optional<String> dependee
  ) {
    TargetInfo source = TargetInfoTest.createTargetInfoWithTargetAddressInfo(type);
    source.getAddressInfos().forEach(s -> s.setIsTargetRoot(is_target_root));

    //getTarget here is actually getting dependencies :/
    if (dependee.isPresent()) {
      targets.entrySet().forEach(s -> {
        if (s.getKey().equals(dependee.get())){
          s.getValue().addDependency(targetAddress);
        }
      });
    }
    targets.put(targetAddress, source);
  }
}
