// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.completion;

public class PantsCompletionTest extends PantsCompletionTestBase {
  public PantsCompletionTest() {
    super("completion");
  }

  public void testTargetName1() throws Throwable {
    myFixture.addFileToProject("foo/BUILD", "jar_library(name='bar')\njar_library(name='baz')");
    configure();
    doTestVariants();
  }

  public void testTargetName2() throws Throwable {
    myFixture.addFileToProject("foo/BUILD", "jar_library(name='bar')\njar_library(name='baz')");
    configure();
    doTestVariants();
  }

  public void testTargetName3() throws Throwable {
    configure("foo");
    doTestVariants();
  }

  public void testTargetName4() throws Throwable {
    configure("foo");
    doTestVariants();
  }

  public void testTargetName5() throws Throwable {
    myFixture.addFileToProject("foo/BUILD", "jar_library(name='bar')\njar_library(name='baz')");
    configure();
    doTestVariants();
  }

  public void testTargets() throws Throwable {
    configure();
    doTestVariants();
  }

  public void testTargetPath1() throws Throwable {
    myFixture.addFileToProject("bar/BUILD", "");
    myFixture.addFileToProject("baz/BUILD", "");
    configure();
    doTestVariants();
  }

  public void testTargetPath2() throws Throwable {
    myFixture.addFileToProject("bar/BUILD", "");
    myFixture.addFileToProject("baz/BUILD", "");
    configure("foo"); // completion from foo/BUILD
    doTestVariants();
  }

  public void testTargetPath3() throws Throwable {
    myFixture.addFileToProject("bar/BUILD", "");
    myFixture.addFileToProject("baz/BUILD", "");
    configure("foo");
    doTestVariants();
  }

  public void testTargetPath4() throws Throwable {
    myFixture.addFileToProject("bar/BUILD", "");
    myFixture.addFileToProject("baz/BUILD", "");
    configure("foo");
    doCompletionTest('\n');
  }

  public void testTargetPath5() throws Throwable {
    myFixture.addFileToProject("bar/BUILD", "");
    myFixture.addFileToProject("baz/BUILD", "");
    configure("foo");
    doCompletionTest('\n');
  }

  public void testTargetPath6() throws Throwable {
    myFixture.addFileToProject("bar/BUILD", "");
    myFixture.addFileToProject("baz/BUILD", "");
    configure("foo");
    doCompletionTest('\n');
  }

  public void testTargetPath7() throws Throwable {
    myFixture.addFileToProject("foo/bar/baz/BUILD", "");
    myFixture.addFileToProject("foo/baz/BUILD", "");
    configure("foo");
    doCompletionTest('\n');
  }

  public void testTargetPath8() throws Throwable {
    myFixture.addFileToProject("foo/bar/baz/BUILD", "jar_library(name='bar')\njar_library(name='baz')");
    configure("foo");
    doCompletionTest('\n');
  }
}
