// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.resolve;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.twitter.intellij.pants.testFramework.PantsCodeInsightFixtureTestCase;

import java.util.Collection;

public class PantsResolveTest extends PantsCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/resolve";
  }

  protected Collection<PsiElement> doTest(int expectedSize) {
    final PsiFile file = myFixture.getFile();
    assertNotNull(file);
    final PsiReference reference = file.findReferenceAt(myFixture.getCaretOffset());
    assertNotNull("no reference", reference);
    final Collection<PsiElement> elements = TargetElementUtil.getInstance().getTargetCandidates(reference);
    assertNotNull(elements);
    assertEquals(expectedSize, elements.size());
    return elements;
  }

  public void testScalaLibrary() {
    myFixture.configureByText("BUILD", "scala_lib<caret>rary()");
    doTest(1);
  }

  public void testDependencies() {
    myFixture.configureByText("BUILD", "depend<caret>encies()");
    doTest(1);
  }

  public void testRGlobs() {
    myFixture.configureByText("BUILD", "scala_library(sources=rg<caret>lobs('src/*.java'))");
    doTest(1);
  }

  public void testDependencies1() {
    setUpPantsExecutable();
    PsiFile expectedTarget = myFixture.addFileToProject("foo/bar/BUILD", "");
    myFixture.configureByText("BUILD", "scala_library(dependencies=['foo/ba<caret>r']");
    PsiElement target = doTest(1).iterator().next();
    assertTrue("Expected a directory!", target instanceof PsiDirectory);
    assertEquals("Wrong directory result", expectedTarget, target);
  }

  public void testDependencies2() {
    setUpPantsExecutable();
    myFixture.addFileToProject("foo/bar/BUILD", "");
    myFixture.configureByText("BUILD", "scala_library(dependencies=['fo<caret>o/bar']");
    PsiElement element = doTest(1).iterator().next();
    assertTrue("Expected a directory!", element instanceof PsiDirectory);
    assertEquals("Wrong directory name", "foo", ((PsiDirectory) element).getName());
  }
}
