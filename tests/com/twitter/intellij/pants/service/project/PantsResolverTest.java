package com.twitter.intellij.pants.service.project;

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
    asserSourceRoot("src__common_packages", "src/com/foo/bar");
    asserSourceRoot("src__common_packages", "src/com/foo/baz");
  }
}
