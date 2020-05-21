// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.bsp.FastpassTargetListCache;
import com.twitter.intellij.pants.bsp.FastpassUtils;
import com.twitter.intellij.pants.bsp.InvalidTargetException;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;

public class BspPantsIntegrationTest extends OSSPantsIntegrationTest {
  FastpassTargetListCache cache;

  public void testFileSync() throws Throwable {
    doImport("examples/tests/java/org/pantsbuild/example/hello");
    cache = new FastpassTargetListCache(Paths.get(myProjectRoot.getPath()));

    // Test flat directory rule
    Map<PantsTargetAddress, Collection<PantsTargetAddress>> parsedEmptyFlatDirectoryRule =
      parse(Collections.singleton("examples/tests/java/org/pantsbuild/example/hello:"));
    assertEquals(1, parsedEmptyFlatDirectoryRule.size());
    assertEquals(0, parsedEmptyFlatDirectoryRule.values().stream().findFirst().get().size());

    // Test flat directory rule
    Map<PantsTargetAddress, Collection<PantsTargetAddress>> parsedNonEmptyFlatDirectoryRule =
      parse(Collections.singleton("examples/tests/java/org/pantsbuild/example/hello/greet:"));
    assertEquals(1, parsedNonEmptyFlatDirectoryRule.size());
    assertEquals(1, parsedNonEmptyFlatDirectoryRule.values().stream().findFirst().get().size());

    // Test deep directory rule
    Map<PantsTargetAddress, Collection<PantsTargetAddress>> parsedDeepDirectoryRule =
      parse(Collections.singleton("examples/tests/java/org/pantsbuild/example/hello::"));
    assertEquals(1, parsedDeepDirectoryRule.size());
    assertEquals(1, parsedDeepDirectoryRule.values().stream().findFirst().get().size());

    // Test single target rule
    Map<PantsTargetAddress, Collection<PantsTargetAddress>> parsedSingeTargetRule =
      parse(Collections.singleton("examples/tests/java/org/pantsbuild/example/hello/greet:greet"));
    assertEquals(1, parsedSingeTargetRule.size());
    assertEquals(1, parsedSingeTargetRule.values().stream().findFirst().get().size());

    // Throws for malformed target
    assertThrows(
      InvalidTargetException.class,
      "Malformed address",
      () -> parse(Collections.singleton("examples/tests/java/org/pantsbuild/example/hello/greet"))
    );

    // Throws for non-existing folder
    assertThrows(
      InvalidTargetException.class,
      "No such folder",
      () -> parse(Collections.singleton("examples/tests/java/org/pantsbuild/example/hello/greet1:"))
    );

    // Throws for non-existing target
    assertThrows(
      InvalidTargetException.class,
      "No such target",
      () -> parse(Collections.singleton("examples/tests/java/org/pantsbuild/example/hello/greet:greet1"))
    );
  }

  private Map<PantsTargetAddress, Collection<PantsTargetAddress>> parse(Set<String> rules)
    throws Throwable {
    try {
      return FastpassUtils.validateAndGetPreview(
        myProjectRoot,
        rules,
        cache::getTargetsList
      ).join();
    } catch (CompletionException e) {
      throw e.getCause();
    }
  }
}

