// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.twitter.intellij.pants.base.PantsCodeInsightFixtureTestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class PantsPsiUtilTest extends PantsCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/util";
  }

  public void doFindTargetsTest(List<Target> actualTargets) {
    final VirtualFile buildFile = myFixture.copyFileToProject(getTestName(true) + ".py", "BUILD");
    myFixture.configureFromExistingVirtualFile(buildFile);
    final List<Target> targets = PantsPsiUtil.findTargets(myFixture.getFile());
    assertOrderedEquals(actualTargets, targets);
  }

  public void testSourceTypeForTargetType() {
    assertEquals(
      "Source type correctly set",
      PantsSourceType.SOURCE,
      PantsUtil.getSourceTypeForTargetType("source")
    );

    assertEquals(
      "Resource type correctly set",
      PantsSourceType.RESOURCE,
      PantsUtil.getSourceTypeForTargetType("resource")
    );

    assertEquals(
      "Test Source type correctly set",
      PantsSourceType.TEST,
      PantsUtil.getSourceTypeForTargetType("TEST")
    );

    assertEquals(
      "Test Resource type correctly set",
      PantsSourceType.TEST_RESOURCE,
      PantsUtil.getSourceTypeForTargetType("TEST_RESOURCE")
    );

    assertEquals(
      "Source type correctly set for gibberish",
      PantsSourceType.SOURCE,
      PantsUtil.getSourceTypeForTargetType("gibberish")
    );
  }

  public void testFindTargets() {
    final List<Target> testTargets = Arrays.asList(new Target("main", "jvm_app"), new Target("main-bin", "jvm_binary"));
    doFindTargetsTest(testTargets);
  }

  public void testWeirdBuildFile() {
    final List<Target> testTargets = Arrays.asList();
    doFindTargetsTest(testTargets);
  }

  public void testTrickyBuildFile() {
    final List<Target> testTargets = Arrays.asList(new Target("main", "jvm_app"), new Target("main-bin", "jvm_binary"));
    doFindTargetsTest(testTargets);
  }

  public void testAliases() {
    final PsiFile psiFile = myFixture.configureByFile("aliases.py");
    assertTrue(psiFile instanceof PyFile);
    final Map<String, PyReferenceExpression> definitions = PantsPsiUtil.findTargetDefinitions((PyFile)psiFile);
    assertContainsElements(
      definitions.keySet(),
      Arrays.asList("annotation_processor", "jar_library", "scala_library", "scalac_plugin", "rglobs")
    );
  }
}
