// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsAction;
import com.twitter.intellij.pants.compiler.actions.PantsCompileAllTargetsInModuleAction;
import com.twitter.intellij.pants.compiler.actions.PantsCompileCurrentTargetAction;
import com.twitter.intellij.pants.compiler.actions.PantsCompileTargetAction;
import com.twitter.intellij.pants.compiler.actions.PantsRebuildAction;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class OSSPantsCompileActionsTest extends OSSPantsIntegrationTest {

  private final DataContext PANTS_PROJECT_DATA = s -> s.equals("project") ? myProject : null;

  public void testCompileAllAction() throws Throwable {
    doImport("testprojects/src/java/org/pantsbuild/testproject/annotation");
    PantsCompileAllTargetsAction compileAllTargetsAction = new PantsCompileAllTargetsAction();
    Set<String> targetAddresses = compileAllTargetsAction.getTargets(getPantsActionEvent(), myProject).collect(Collectors.toSet());
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
    Set<String> targetAddresses = compileTargetAction.getTargets(getPantsActionEvent(), myProject).collect(Collectors.toSet());
    Set<String> expectedTarget = new HashSet<>(Arrays.asList("testprojects/src/java/org/pantsbuild/testproject/annotation/main:main"));
    assertEquals(expectedTarget, targetAddresses);
    assertFalse(compileTargetAction.doCleanAll());
  }

  public void testRebuildAction() throws Throwable {
    doImport("testprojects/src/java/org/pantsbuild/testproject/annotation");
    PantsRebuildAction rebuildAction = new PantsRebuildAction();
    Set<String> targetAddresses = rebuildAction.getTargets(getPantsActionEvent(), myProject).collect(Collectors.toSet());
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
    Module module = getModule("testprojects_src_java_org_pantsbuild_testproject_junit_testscope_common_sources");
    PantsCompileAllTargetsInModuleAction compileAllTargetsInModuleAction = new PantsCompileAllTargetsInModuleAction(module);
    Set<String> targetAddresses = compileAllTargetsInModuleAction.getTargets(getPantsActionEvent(), myProject).collect(Collectors.toSet());
    Set<String> expectedTargets = new HashSet<>(Arrays.asList(
      "testprojects/src/java/org/pantsbuild/testproject/junit/testscope:tests",
      "testprojects/src/java/org/pantsbuild/testproject/junit/testscope:check",
      "testprojects/src/java/org/pantsbuild/testproject/junit/testscope:lib",
      "testprojects/src/java/org/pantsbuild/testproject/junit/testscope:bin"
    ));
    assertEquals(expectedTargets, targetAddresses);
    assertFalse(compileAllTargetsInModuleAction.doCleanAll());
  }

  public void testCompileTargetsInSelectedEditor() throws Throwable {
    doImport("examples/tests/scala/org/pantsbuild/example");

    ArrayList<Pair<String, String>> testClassAndTarget = Lists.newArrayList(
      // Pair of class reference and the target it belong to
      Pair.create("org.pantsbuild.example.hello.welcome.WelSpec", "examples/tests/scala/org/pantsbuild/example/hello/welcome:welcome"),
      Pair.create("org.pantsbuild.example.specs2.HelloWorldSpec", "examples/tests/scala/org/pantsbuild/example/specs2:specs2")
    );

    for (Pair<String, String> classAndTarget : testClassAndTarget) {
      String clazzName = classAndTarget.getFirst();
      String target = classAndTarget.getSecond();

      PsiClass clazz = JavaPsiFacade.getInstance(myProject)
        .findClass(clazzName, GlobalSearchScope.projectScope(myProject));

      assertNotNull(String.format("%s does not exist, but should.", clazz), clazz);

      FileEditorManager.getInstance(myProject).openFile(clazz.getContainingFile().getVirtualFile(), true);
      PantsCompileCurrentTargetAction compileCurrentTargetAction = new PantsCompileCurrentTargetAction();
      Set<String> currentTargets = compileCurrentTargetAction.getTargets(getPantsActionEvent(), myProject).collect(Collectors.toSet());
      assertEquals(Sets.newHashSet(target), currentTargets);
    }
  }

  private AnActionEvent getPantsActionEvent() {
    return AnActionEvent.createFromDataContext("", null, PANTS_PROJECT_DATA);
  }
}
