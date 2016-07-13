// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import net.miginfocom.layout.AC;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;


public class OSSPantsActionOverrideTest extends OSSPantsIntegrationTest {

  final String PANTS_ON = "actively overriding";

  public void testPantsRebuildOverride() throws Throwable {
    makeActionOverrideTest(
      IdeActions.ACTION_COMPILE_PROJECT,
      true,
      "PantsRebuildAction",
      "testprojects/tests/java/org/pantsbuild/testproject/annotation"
    );
  }

  public void testNonPantsRebuildOverride() throws Throwable {
    makeActionOverrideTest(
      IdeActions.ACTION_COMPILE_PROJECT,
      false,
      "PantsRebuildAction"
    );
  }

  public void testPantsCompileOverride() throws Throwable {
    makeActionOverrideTest(
      IdeActions.ACTION_COMPILE,
      true,
      "PantsShieldAction",
      "testprojects/tests/java/org/pantsbuild/testproject/cwdexample"
    );
  }

  public void testPantsNonCompileOverride() throws Throwable {
    makeActionOverrideTest(
      IdeActions.ACTION_COMPILE,
      false,
      "PantsShieldAction"
    );
  }

  public void testPantsMakeOverride() throws Throwable {
    makeActionOverrideTest(
      IdeActions.ACTION_MAKE_MODULE,
      true,
      "PantsCompileTargetAction",
      "testprojects/tests/java/org/pantsbuild/testproject/matcher"
    );
  }

  public void testNonPantsMakeOverride() throws Throwable {
    makeActionOverrideTest(
      IdeActions.ACTION_MAKE_MODULE,
      false,
      "PantsCompileTargetAction"
    );
  }

  public void testPantsCompileAllOverride() throws Throwable {
    makeActionOverrideTest(
      PantsConstants.ACTION_MAKE_PROJECT_ID,
      true,
      "PantsCompileAllTargetsAction",
      "testprojects/tests/java/org/pantsbuild/testproject/unicode"
    );
  }

  public void testNonPantsCompileAllOverride() throws Throwable {
    makeActionOverrideTest(
      PantsConstants.ACTION_MAKE_PROJECT_ID,
      false,
      "PantsCompileAllTargetsAction"
    );
  }

  private void makeActionOverrideTest(String actionId, boolean isPantsProject, String pantsActionClassName, String... projectPaths) {
    if (isPantsProject) {
      for (String path : projectPaths) {
        doImport(path);
      }
    }

    ActionManager actionManager = ActionManager.getInstance();
    AnAction action = actionManager.getAction(actionId);
    action.update(AnActionEvent.createFromDataContext("menu", null, s -> s.equals("project") ? myProject : null));
    String actionString = action.toString();

    if (isPantsProject) {
      assertTrue(actionString, actionString.contains(PANTS_ON));
    } else {
      assertFalse(actionString, actionString.contains(PANTS_ON));
    }
    assertTrue(actionString, actionString.contains(pantsActionClassName));
  }
}
