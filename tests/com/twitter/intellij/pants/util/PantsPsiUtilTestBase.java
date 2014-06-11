package com.twitter.intellij.pants.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.base.PantsCodeInsightFixtureTestCase;
import com.twitter.intellij.pants.util.PantsPsiUtil;
import com.twitter.intellij.pants.util.Target;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ajohnson on 6/10/14.
 */
public class PantsPsiUtilTestBase extends PantsCodeInsightFixtureTestCase {

  private final String myPath;

  public PantsPsiUtilTestBase(String... path) {
    myPath = getPath(path);
  }

  @Override
  protected String getBasePath() {
    return myPath;
  }

  private static String getPath(String... args) {
    final StringBuilder result = new StringBuilder();
    for (String folder : args) {
      result.append("/");
      result.append(folder);
    }
    return result.toString();
  }

  public void doTest(List<Target> actualTargets) {
    doTest(null, actualTargets);
  }

  public void doTest(String targetPath, List<Target> actualTargets) {
    final String buildPath = targetPath == null ? "BUILD" : targetPath + "/BUILD";
    final VirtualFile buildFile = myFixture.copyFileToProject(getTestName(true) + ".py", buildPath);
    myFixture.configureFromExistingVirtualFile(buildFile);
    final List<Target> targets = PantsPsiUtil.findTargets(myFixture.getFile());
    assertOrderedEquals(actualTargets, targets);
  }

}
