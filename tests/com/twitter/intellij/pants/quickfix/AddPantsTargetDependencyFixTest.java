// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;

import com.intellij.openapi.command.WriteCommandAction;
import com.twitter.intellij.pants.testFramework.PantsCodeInsightFixtureTestCase;
import com.twitter.intellij.pants.model.PantsTargetAddress;

public class AddPantsTargetDependencyFixTest extends PantsCodeInsightFixtureTestCase {
  public AddPantsTargetDependencyFixTest() {
    super("quickfix", "addPantsTargetDependency");
  }

  private void doTest(final String targetName, String addressToAdd) {
    final String testName = getTestName(true);
    myFixture.configureByFile(testName + ".py");

    final PantsTargetAddress address = new PantsTargetAddress("test/path", targetName);
    final PantsTargetAddress dependencyAddress = PantsTargetAddress.fromString(addressToAdd);
    final AddPantsTargetDependencyFix dependencyFix = new AddPantsTargetDependencyFix(address, dependencyAddress);

    WriteCommandAction.Simple.runWriteCommandAction(
      getProject(),
      new Runnable() {
        @Override
        public void run() {
          dependencyFix.doInsert(myFixture.getFile(), targetName, dependencyAddress);
        }
      }
    );
    myFixture.checkResultByFile(testName + "_expected.py");
  }

  public void testBasic() {
    doTest("test", "bar/baz3");
  }

  public void testEmpty() {
    doTest("missingdepswhitelist2", "foo/bar/baz:tests");
  }

  public void testLast() {
    doTest("test", "bar/baz3");
  }
}
