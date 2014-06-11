package com.twitter.intellij.pants.util;

/**
 * Created by ajohnson on 6/10/14.
 */
public class PantsPsiUtilTest extends PantsPsiUtilTestBase {
  public PantsPsiUtilTest() {
    super("pantsPsiUtil");
  }

  public void testFindTargets() {
    doTest(2);
  }

  public void testWeirdBuildFile() {
    doTest(0);
  }

  public void testTrickyBuildFile() {
    doTest(2);
  }

}
