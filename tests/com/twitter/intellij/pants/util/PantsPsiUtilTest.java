package com.twitter.intellij.pants.util;

import java.util.Arrays;
import java.util.List;

/**
 * Created by ajohnson on 6/10/14.
 */
public class PantsPsiUtilTest extends PantsPsiUtilTestBase {
  public PantsPsiUtilTest() {
    super("pantsPsiUtil");
  }

  public void testFindTargets() {
    final List<Target> testTargets = Arrays.asList(new Target("main", "jvm_app"), new Target("main-bin", "jvm_binary"));
    doTest(testTargets);
  }

  public void testWeirdBuildFile() {
    final List<Target> testTargets = Arrays.asList();
    doTest(testTargets);
  }

  public void testTrickyBuildFile() {
    final List<Target> testTargets = Arrays.asList(new Target("main", "jvm_app"), new Target("main-bin", "jvm_binary"));
    doTest(testTargets);
  }

}
