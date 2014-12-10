// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.openapi.util.text.CharFilter;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.UsefulTestCase;
import com.twitter.intellij.pants.testFramework.PantsCodeInsightFixtureTestCase;
import org.jetbrains.annotations.Nullable;

import java.util.*;

abstract public class PantsCompletionTestBase extends PantsCodeInsightFixtureTestCase {
  enum CheckType {EQUALS, INCLUDES, EXCLUDES}

  public PantsCompletionTestBase(String... path) {
    super(path);
  }

  protected void configure() throws Throwable {
    configure(null);
  }

  protected void configure(@Nullable String targetPath) throws Throwable {
    setUpPantsExecutable();
    final String buildPath = targetPath == null ? "BUILD" : targetPath + "/BUILD";
    final VirtualFile buildFile = myFixture.copyFileToProject(getTestName(true) + ".py", buildPath);
    myFixture.configureFromExistingVirtualFile(buildFile);
  }

  protected void doCompletionTest(char ch) {
    myFixture.complete(CompletionType.BASIC);
    myFixture.finishLookup(ch);
    myFixture.checkResultByFile(getTestName(true) + "_expected.py");
  }

  protected void doTestVariants() throws Throwable {
    doTestVariantsInner(getTestName(true) + ".txt");
  }

  protected void doTestVariantsInner(String fileName) throws Throwable {
    final VirtualFile virtualFile = myFixture.copyFileToProject(fileName);
    final Scanner in = new Scanner(virtualFile.getInputStream());

    final CompletionType type = CompletionType.valueOf(in.next());
    final int count = in.nextInt();
    final CheckType checkType = CheckType.valueOf(in.next());

    final List<String> variants = new ArrayList<String>();
    while (in.hasNext()) {
      final String variant = StringUtil.strip(in.next(), CharFilter.NOT_WHITESPACE_FILTER);
      if (variant.length() > 0) {
        variants.add(variant);
      }
    }

    myFixture.complete(type, count);
    checkCompletion(checkType, variants);
  }

  protected void checkCompletion(CheckType checkType, String... variants) {
    checkCompletion(checkType, new ArrayList<String>(Arrays.asList(variants)));
  }

  protected void checkCompletion(CheckType checkType, List<String> variants) {
    List<String> stringList = myFixture.getLookupElementStrings();
    if (stringList == null) {
      stringList = Collections.emptyList();
    }

    if (checkType == CheckType.EQUALS) {
      UsefulTestCase.assertSameElements(stringList, variants);
    }
    else if (checkType == CheckType.INCLUDES) {
      variants.removeAll(stringList);
      assertTrue("Missing variants: " + variants, variants.isEmpty());
    }
    else if (checkType == CheckType.EXCLUDES) {
      variants.retainAll(stringList);
      assertTrue("Unexpected variants: " + variants, variants.isEmpty());
    }
  }
}
