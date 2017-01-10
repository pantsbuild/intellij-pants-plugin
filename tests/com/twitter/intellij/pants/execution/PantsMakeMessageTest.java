// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import junit.framework.TestCase;

import java.util.Optional;

import static com.twitter.intellij.pants.execution.PantsMakeBeforeRun.ERROR_TAG;

public class PantsMakeMessageTest extends TestCase {

  public void testErrorMessageWithLocation() {
    Optional<PantsMakeBeforeRun.ParseResult> result = PantsMakeBeforeRun.parseErrorLocation(
      "                       [error] /Users/dummy/workspace/pants_ij/examples/tests/java/org/pantsbuild/example/hello/greet tv/GreetingTest.java:23:1: cannot find symbol",
      ".java",
      ERROR_TAG
    );
    assertTrue(result.isPresent());
    assertEquals(
      "/Users/dummy/workspace/pants_ij/examples/tests/java/org/pantsbuild/example/hello/greet tv/GreetingTest.java",
      result.get().getFilePath()
    );
    assertEquals(23, result.get().getLineNumber());
    assertEquals(1, result.get().getColumnNumber());
  }


  public void testErrorMessageWithLocation2() {
    Optional<PantsMakeBeforeRun.ParseResult> result = PantsMakeBeforeRun.parseErrorLocation(
      "                       [error] /a/b/c/d/e/f/g.scala:23:1: cannot find symbol",
      ".scala",
      ERROR_TAG
    );
    assertTrue(result.isPresent());
    assertEquals(
      "/a/b/c/d/e/f/g.scala",
      result.get().getFilePath()
    );
    assertEquals(23, result.get().getLineNumber());
    assertEquals(1, result.get().getColumnNumber());
  }

  public void testErrorMessageWithNoLocation() {
    Optional<PantsMakeBeforeRun.ParseResult> result = PantsMakeBeforeRun.parseErrorLocation(
      "                       [error]     String greeting = Greeting.greetFromRXesource(\"org/pantsbuild/example/hello/world.txt\");\n",
      ".java",
      ERROR_TAG
    );
    assertFalse(result.isPresent());
  }
}
