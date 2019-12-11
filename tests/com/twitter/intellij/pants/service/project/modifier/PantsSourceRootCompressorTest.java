// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.modifier;

import junit.framework.TestCase;
import org.apache.commons.compress.utils.Sets;

import java.io.File;

public class PantsSourceRootCompressorTest extends TestCase {
  public void testFindAncestors1() {
    assertEquals(
      Sets.newHashSet(new File("a/b/c")),
      PantsSourceRootCompressor.findAncestors(Sets.newHashSet(
        new File("a/b/c"),
        new File("a/b/c/d"),
        new File("a/b/c/e"),
        new File("a/b/c/f/g")
      ))
    );
  }

  public void testFindAncestors2() {
    assertEquals(
      Sets.newHashSet(new File("a/b/c"), new File("a/b/e")),
      PantsSourceRootCompressor.findAncestors(Sets.newHashSet(
        new File("a/b/c"),
        new File("a/b/c/d"),
        new File("a/b/e")
      ))
    );
  }
}
