package com.twitter.intellij.pants.completion;

public class PantsCompletionTest extends PantsCompletionTestBase {
  public PantsCompletionTest() {
    super("completion");
  }

  public void testTargets() throws Throwable {
    doTest();
  }
}
