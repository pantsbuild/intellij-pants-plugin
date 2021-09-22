// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.testFramework.UsefulTestCase;

import java.nio.file.Paths;
import java.util.Optional;

public class PantsTargetAddressTest extends UsefulTestCase {
  public void testDirectEntry() {
    Optional<PantsTargetAddress> t = PantsTargetAddress.tryParse("project:target");
    assertEquals(t.get(), PantsTargetAddress.oneTargetInDir(Paths.get("project"), "target"));
  }

  public void testRecursiveEntry() {
    Optional<PantsTargetAddress> t = PantsTargetAddress.tryParse("project::");
    assertEquals(t.get(), PantsTargetAddress.allTargetsInDirDeep(Paths.get("project")));
  }

  public void testFlatEntry() {
    Optional<PantsTargetAddress> t = PantsTargetAddress.tryParse("project:");
    assertEquals(t.get(), PantsTargetAddress.allTargetsInDirFlat(Paths.get("project")));
  }

  public void testJustPath() {
    Optional<PantsTargetAddress> t = PantsTargetAddress.tryParse("a/b/c");
    assertEquals(t.get(), PantsTargetAddress.oneTargetInDir(Paths.get("a/b/c"), "c"));
  }

  public void testJustColon() {
    Optional<PantsTargetAddress> t = PantsTargetAddress.tryParse(":");
    assertFalse(t.isPresent());
  }

  public void testJustDoubleColon() {
    Optional<PantsTargetAddress> t = PantsTargetAddress.tryParse("::");
    assertFalse(t.isPresent());
  }
}
