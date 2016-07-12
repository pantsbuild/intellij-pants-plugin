// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.ui.PantsCompileAllTargetsAction;
import com.twitter.intellij.pants.ui.PantsRebuildAction;
import com.twitter.intellij.pants.util.PantsConstants;


public class OSSPantsActionOverrideTest extends OSSPantsIntegrationTest {

  public void testPantsRebuildOverride() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/annotation");

    ActionManager actionManager = ActionManager.getInstance();
    AnAction rebuildAction = actionManager.getAction(IdeActions.ACTION_COMPILE_PROJECT);
    String actionString  = rebuildAction.toString();

    assertTrue(actionString, actionString.contains("overriding"));
    assertTrue(actionString, actionString.contains("PantsRebuildAction"));
  }

  public void testPantsCompileOverride() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/cwdexample");

    ActionManager actionManager = ActionManager.getInstance();
    AnAction compileAction = actionManager.getAction(IdeActions.ACTION_COMPILE);
    String actionString  = compileAction.toString();

    assertTrue(actionString, actionString.contains("overriding"));
    assertTrue(actionString, actionString.contains("PantsShieldAction"));
  }

  public void testPantsMakeOverride() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/matcher");

    ActionManager actionManager = ActionManager.getInstance();
    AnAction makeAction = actionManager.getAction(IdeActions.ACTION_MAKE_MODULE);
    String actionString  = makeAction.toString();

    assertTrue(actionString, actionString.contains("overriding"));
    assertTrue(actionString, actionString.contains("PantsCompileTargetAction"));
  }

  public void testPantsCompileAllOverride() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/unicode");

    ActionManager actionManager = ActionManager.getInstance();
    AnAction compileAllAction = actionManager.getAction(PantsConstants.ACTION_MAKE_PROJECT_ID);
    String actionString  = compileAllAction.toString();

    assertTrue(actionString, actionString.contains("overriding"));
    assertTrue(actionString, actionString.contains("PantsCompileAllTargetsAction"));
  }
}
