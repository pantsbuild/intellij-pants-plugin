// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsAction;
import com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsInModuleAction;
import com.twitter.intellij.pants.compiler.actions.PantsCompileTargetAction;
import com.twitter.intellij.pants.compiler.actions.PantsRebuildAction;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class OSSPantsCompileActionsTest extends OSSPantsIntegrationTest {

  private final DataContext PANTS_PROJECT_DATA = s -> s.equals("project") ? myProject : null;

  public void testCompileAllAction() throws Throwable {
    doImport("testprojects/src/java/org/pantsbuild/testproject/annotation");
    PantsCompileAllTargetsAction compileAllTargetsAction = new PantsCompileAllTargetsAction();
    Stream<String> rawTargets = compileAllTargetsAction.getTargets(getPantsActionEvent(), myProject);
    assertNotNull(rawTargets);
    Set<String> targetAddresses = rawTargets.collect(Collectors.toSet());
    Set<String> expectedTargets = new HashSet<>(Arrays.asList(
      "testprojects/src/java/org/pantsbuild/testproject/annotation/processor:processor",
      "testprojects/src/java/org/pantsbuild/testproject/annotation/processorwithdep/hellomaker:hellomaker",
      "testprojects/src/java/org/pantsbuild/testproject/annotation/processorwithdep/processor:processor",
      "testprojects/src/java/org/pantsbuild/testproject/annotation/processorwithdep/main:main",
      "testprojects/src/java/org/pantsbuild/testproject/annotation/main:main"
    ));
    assertEquals(expectedTargets, targetAddresses);
    assertFalse(compileAllTargetsAction.doCleanAll());
  }

  public void testCompileTargetAction() throws Throwable {
    doImport("testprojects/src/java/org/pantsbuild/testproject/annotation");
    PantsCompileTargetAction compileTargetAction =
      new PantsCompileTargetAction("testprojects/src/java/org/pantsbuild/testproject/annotation/main:main");
    Stream<String> rawTargets = compileTargetAction.getTargets(getPantsActionEvent(), myProject);
    assertNotNull(rawTargets);
    Set<String> targetAddresses = rawTargets.collect(Collectors.toSet());
    Set<String> expectedTarget = new HashSet<>(Arrays.asList("testprojects/src/java/org/pantsbuild/testproject/annotation/main:main"));
    assertEquals(expectedTarget, targetAddresses);
    assertFalse(compileTargetAction.doCleanAll());
  }

  public void testRebuildAction() throws Throwable {
    doImport("testprojects/src/java/org/pantsbuild/testproject/annotation");
    PantsRebuildAction rebuildAction = new PantsRebuildAction();
    Stream<String> rawTargets = rebuildAction.getTargets(getPantsActionEvent(), myProject);
    assertNotNull(rawTargets);
    Set<String> targetAddresses = rawTargets.collect(Collectors.toSet());
    Set<String> expectedTargets = new HashSet<>(Arrays.asList(
      "testprojects/src/java/org/pantsbuild/testproject/annotation/processor:processor",
      "testprojects/src/java/org/pantsbuild/testproject/annotation/processorwithdep/hellomaker:hellomaker",
      "testprojects/src/java/org/pantsbuild/testproject/annotation/processorwithdep/processor:processor",
      "testprojects/src/java/org/pantsbuild/testproject/annotation/processorwithdep/main:main",
      "testprojects/src/java/org/pantsbuild/testproject/annotation/main:main"
    ));
    assertEquals(expectedTargets, targetAddresses);
    assertTrue(rebuildAction.doCleanAll());
  }

  public void testCompileAllTargetsInModuleAction() throws Throwable {
    doImport("testprojects/src/java/org/pantsbuild/testproject/junit");
    assertModuleExists("_testprojects_src_java_org_pantsbuild_testproject_junit_testscope_common_sources");
    Module module = getModule("_testprojects_src_java_org_pantsbuild_testproject_junit_testscope_common_sources");
    PantsCompileAllTargetsInModuleAction compileAllTargetsInModuleAction = new PantsCompileAllTargetsInModuleAction(module);
    Stream<String> rawTargets = compileAllTargetsInModuleAction.getTargets(getPantsActionEvent(), myProject);
    assertNotNull(rawTargets);
    Set<String> targetAddresses = rawTargets.collect(Collectors.toSet());
    Set<String> expectedTargets = new HashSet<>(Arrays.asList(
      "testprojects/src/java/org/pantsbuild/testproject/junit/testscope:tests",
      "testprojects/src/java/org/pantsbuild/testproject/junit/testscope:check",
      "testprojects/src/java/org/pantsbuild/testproject/junit/testscope:lib",
      "testprojects/src/java/org/pantsbuild/testproject/junit/testscope:bin"
    ));
    assertEquals(expectedTargets, targetAddresses);
    assertFalse(compileAllTargetsInModuleAction.doCleanAll());
  }

  private AnActionEvent getPantsActionEvent() {
    return AnActionEvent.createFromDataContext("", null, PANTS_PROJECT_DATA);
  }
}
