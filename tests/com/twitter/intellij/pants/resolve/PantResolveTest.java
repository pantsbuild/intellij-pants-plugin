package com.twitter.intellij.pants.resolve;

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.twitter.intellij.pants.base.PantsCodeInsightFixtureTestCase;

import java.util.Collection;

public class PantResolveTest extends PantsCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/resolve";
  }

  protected Collection<PsiElement> doTest(int expectedSize) {
    PsiFile file = myFixture.getFile();
    assertNotNull(file);
    PsiReference reference = file.findReferenceAt(myFixture.getCaretOffset());
    assertNotNull("no reference", reference);
    final Collection<PsiElement> elements = TargetElementUtilBase.getInstance().getTargetCandidates(reference);
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
}
