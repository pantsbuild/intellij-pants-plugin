package com.twitter.intellij.pants.service.project;

public class PantsResolverTest extends PantsResolverTestBase {
  public void testCommonRoots1() {
    addInfo("a:java").
      withDefaultRoot("com/foo/bar").
      withDefaultRoot("com/foo/baz");
    addInfo("b:scala").
      withDefaultRoot("com/foo/bar").
      withDefaultRoot("com/foo/baz");
    addInfo("c:tests").
      withDefaultRoot("com/foo/baz");

    assertDependency("a_java", "com.foo.bar");
    assertDependency("a_java", "c_tests");
    assertDependency("b_scala", "com.foo.bar");
    assertDependency("b_scala", "c_tests");
  }
}
