// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.twitter.intellij.pants.testFramework.PantsCodeInsightFixtureTestCase;
import com.twitter.intellij.pants.model.PantsSourceType;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class PantsPsiUtilTest extends PantsCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/util";
  }

  public void doFindTargetsTest(String... actualTargets) {
    final VirtualFile buildFile = myFixture.copyFileToProject(getTestName(true) + ".py", "BUILD");
    myFixture.configureFromExistingVirtualFile(buildFile);
    final Set<String> targetNames = PantsPsiUtil.findTargets(myFixture.getFile()).keySet();
    assertContainsElements(
      targetNames,
      Arrays.asList(actualTargets)
    );
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
    doFindTargetsTest("main", "main-bin");
  }

  public void testWeirdBuildFile() {
    doFindTargetsTest();
  }

  public void testTrickyBuildFile() {
    doFindTargetsTest("main", "main-bin");
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
