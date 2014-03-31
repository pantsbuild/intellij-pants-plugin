package com.twitter.intellij.pants.completion;

public class PantsCompletionTest extends PantsCompletionTestBase {
  public PantsCompletionTest() {
    super("completion");
  }

  public void testTargets() throws Throwable {
    doTest();
  }

  public void testTargetPath1() throws Throwable {
    myFixture.addFileToProject("bar/BUILD", "");
    myFixture.addFileToProject("baz/BUILD", "");
    doTest();
  }

  public void testTargetPath2() throws Throwable {
    myFixture.addFileToProject("bar/BUILD", "");
    myFixture.addFileToProject("baz/BUILD", "");
    doTest("foo"); // completion from foo/BUILD
  }

  public void testTargetPath3() throws Throwable {
    myFixture.addFileToProject("bar/BUILD", "");
    myFixture.addFileToProject("baz/BUILD", "");
    doTest("foo");
  }
}
