// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import java.util.HashSet;
import java.util.Set;

public class PantsResolverTest extends PantsResolverTestBase {
  public void testOneCommonRootOwnedBySingleTargetOneNot() {
    addInfo("a:java").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("b:scala").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("c:tests").
      withRoot("src/com/foo/baz", "com.foo.baz");

    assertDependency("a_java", "src_com_foo_bar_common_sources");
    assertDependency("a_java", "c_tests");
    assertDependency("b_scala", "src_com_foo_bar_common_sources");
    assertDependency("b_scala", "c_tests");
  }

  public void testTargetsWithMultipleCommonRootsEachUseSyntheticTargets() {
    addInfo("a:java").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("b:scala").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("c:tests").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/qwerty", "com.foo.qwerty");

    assertDependency("a_java", "src_com_foo_bar_common_sources");
    assertDependency("a_java", "src_com_foo_baz_common_sources");

    assertDependency("b_scala", "src_com_foo_bar_common_sources");
    assertDependency("b_scala", "src_com_foo_baz_common_sources");

    assertDependency("c_tests", "src_com_foo_bar_common_sources");
  }

  public void testTargetsWithMultipleCommonRootsEachUseSyntheticTargets2() {
    addInfo("a:java").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("b:scala").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("c:tests").
      withRoot("src/com/foo/baz", "com.foo.baz").
      withRoot("src/com/foo/qwerty", "com.foo.qwerty");

    assertDependency("a_java", "src_com_foo_bar_common_sources");
    assertDependency("a_java", "src_com_foo_baz_common_sources");

    assertDependency("b_scala", "src_com_foo_bar_common_sources");
    assertDependency("b_scala", "src_com_foo_baz_common_sources");

    assertDependency("c_tests", "src_com_foo_baz_common_sources");
  }

  public void testTwoTargetsWithTwoCommonRootsHaveNoContentRootsAndDependOnSyntheticTargets() {
    addInfo("a:java").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");
    addInfo("b:scala").
      withRoot("src/com/foo/bar", "com.foo.bar").
      withRoot("src/com/foo/baz", "com.foo.baz");

    assertDependency("a_java", "src_com_foo_bar_common_sources");
    assertDependency("b_scala", "src_com_foo_bar_common_sources");
    assertSourceRoot("src_com_foo_bar_common_sources", "src/com/foo/bar");
    assertSourceRoot("src_com_foo_baz_common_sources", "src/com/foo/baz");

    assertNoContentRoot("a_java");
    assertNoContentRoot("b_scala");
  }

  public void testTargetsWithSingleCommonRootsDependOnSyntheticTarget() {
    addInfo("a:scala").
      withRoot("src/com/foo/bar", "com.foo.bar");
    addInfo("b:scala").
      withRoot("src/com/foo/bar", "com.foo.bar");

    assertNoContentRoot("a_scala");
    assertNoContentRoot("b_scala");
    assertDependency("a_scala", "src_com_foo_bar_common_sources");
    assertDependency("b_scala", "src_com_foo_bar_common_sources");
    assertSourceRoot("src_com_foo_bar_common_sources", "src/com/foo/bar");
  }

  public void testCommonAncestorRoots() {
    addInfo("a:scala").
      withRoot("src/com/foo/a_bar", "com.foo.a_bar").
      withRoot("src/com/foo/a_baz", "com.foo.a_baz");
    addInfo("b:scala").
      withRoot("src/com/foo/b_bar", "com.foo.b_bar").
      withRoot("src/com/foo/b_baz", "com.foo.b_baz");

    assertContentRoots("a_scala",
                       "/src/com/foo/a_bar",
                       "/src/com/foo/a_baz");
    assertContentRoots("b_scala",
                       "/src/com/foo/b_bar",
                       "/src/com/foo/b_baz");
  }

  public void testJavaScalaCyclic() {
    addInfo("a:java").
      withRoot("src/java/foo/bar", "com.foo.bar").
      withRoot("src/java/foo/baz", "com.foo.baz").
      withDependency("a:scala").
      withLibrary("com.twitter.baz");
    addInfo("a:scala").
      withRoot("src/scala/foo/bar", "com.foo.bar").
      withRoot("src/scala/foo/baz", "com.foo.baz").
      withDependency("a:java").
      withLibrary("com.twitter.bar");

    assertModulesCreated("a_java_and_scala");

    assertLibrary("a_java_and_scala", "com.twitter.baz");
    assertLibrary("a_java_and_scala", "com.twitter.bar");

    assertSourceRoot("a_java_and_scala", "src/java/foo/bar");
    assertSourceRoot("a_java_and_scala", "src/java/foo/baz");
    assertSourceRoot("a_java_and_scala", "src/scala/foo/bar");
    assertSourceRoot("a_java_and_scala", "src/scala/foo/baz");
  }
}
