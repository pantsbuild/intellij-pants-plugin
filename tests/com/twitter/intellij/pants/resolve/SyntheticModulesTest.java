// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.resolve;

import com.intellij.testFramework.UsefulTestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.twitter.intellij.pants.service.project.resolver.PantsSourceRootsExtension.*;

public class SyntheticModulesTest extends UsefulTestCase {
  public void testRemoveChildren() {
    HashSet<String> input = new HashSet<>(Arrays.asList("A", "A/B", "A/B/C", "B", "C/D"));
    Set<String> actual = removeChildren(input);
    HashSet<String> expected = new HashSet<>(Arrays.asList("A", "B", "C/D"));
    assertEquals(expected, actual);
  }

  public void testRemoveChildrenEmptySet() {
    HashSet<String> input = new HashSet<>();
    Set<String> actual = removeChildren(input);
    HashSet<String> expected = new HashSet<>();
    assertEquals(expected, actual);
  }
}
