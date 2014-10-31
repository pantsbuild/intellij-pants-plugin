// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import java.util.HashSet;
import java.util.Set;

public class PantsResolverTest extends PantsResolverTestBase {
  public void testCommonRoots1() {
    addInfo("a:java").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("b:scala").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("c:tests").
      withRoot("src/com/foo/baz", "com.foo.baz");

    assertDependency("a_java", "src__com.foo.bar");
    assertDependency("a_java", "c_tests");
    assertDependency("b_scala", "src__com.foo.bar");
    assertDependency("b_scala", "c_tests");
  }

  public void testCommonRoots2() {
    addInfo("a:java").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("b:scala").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("c:tests").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/qwerty", "com.foo.qwerty");

    assertDependency("a_java", "src__com.foo.bar");
    assertDependency("a_java", "src__com.foo.baz");
    assertDependency("b_scala", "src__com.foo.bar");
    assertDependency("b_scala", "src__com.foo.baz");
    assertDependency("c_tests", "src__com.foo.bar");
  }

  public void testCommonRoots3() {
    addInfo("a:java").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("b:scala").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("c:tests").
      withRoot("src/com/foo/baz", "com.foo.baz").
      withRoot("src/com/foo/qwerty", "com.foo.qwerty");

    assertDependency("a_java", "src__com.foo.bar");
    assertDependency("a_java", "src__com.foo.baz");
    assertDependency("b_scala", "src__com.foo.bar");
    assertDependency("b_scala", "src__com.foo.baz");
    assertDependency("c_tests", "src__com.foo.baz");
  }

  public void testCommonRoots4() {
    addInfo("a:java").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("b:scala").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");

    assertDependency("a_java", "src__common_packages");
    assertDependency("b_scala", "src__common_packages");
    assertSourceRoot("src__common_packages", "src/com/foo/bar");
    assertSourceRoot("src__common_packages", "src/com/foo/baz");

    assertNoContentRoot("a_java");
    assertNoContentRoot("b_scala");
  }

  public void testCommonRoots5() {
    addInfo("a:scala").
      withRoot("src/com/foo/bar", "com.foo.bar");
    addInfo("b:scala").
      withRoot("src/com/foo/bar", "com.foo.bar");

    assertNoContentRoot("a_scala");
    assertDependency("a_scala", "b_scala");
    assertSourceRoot("b_scala", "src/com/foo/bar");
  }

  public void testJavaScalaCombined() {
    addInfo("a:java").
      withRoot("src/java/foo/bar", "com.foo.bar").
      withRoot("src/java/foo/baz", "com.foo.baz").
      withLibrary("com.twitter.foo");
    addInfo("b:scala").
      withRoot("src/java/foo/bar", "com.foo.bar").
      withRoot("src/java/foo/baz", "com.foo.baz").
      withRoot("src/scala/foo/baz", "com.foo.baz").
      withLibrary("com.twitter.baz").
      withLibrary("org.scala-lang:scala-library");

    assertSourceRoot("b_scala", "src/java/foo/bar");
    assertSourceRoot("b_scala", "src/java/foo/baz");
    assertSourceRoot("b_scala", "src/scala/foo/baz");
    assertLibrary("b_scala", "com.twitter.foo");
    assertLibrary("b_scala", "com.twitter.baz");
    Set<String> modules = new HashSet<String>();
    modules.add("b_scala");
    assertModulesCreated(modules);
  }
}
