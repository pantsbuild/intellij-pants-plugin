// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.testFramework.UsefulTestCase;

import java.nio.file.Paths;

public class PantsTargetAddressTest extends UsefulTestCase {
  public void testDirectEntry() {
    PantsTargetAddress t = PantsTargetAddress.fromString("project:target");
    assertEquals(t, PantsTargetAddress.oneTargetInDir(Paths.get("project"), "target"));
  }

  public void testRecursiveEntry() {
    PantsTargetAddress t = PantsTargetAddress.fromString("project::");
    assertEquals(t, PantsTargetAddress.allTargetsInDirDeep(Paths.get("project")));
  }

  public void testFlatEntry() {
    PantsTargetAddress t = PantsTargetAddress.fromString("project:");
    assertEquals(t, PantsTargetAddress.allTargetsInDirFlat(Paths.get("project")));
  }
}
