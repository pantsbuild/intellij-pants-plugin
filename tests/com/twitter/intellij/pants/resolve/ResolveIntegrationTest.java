// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.resolve;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.twitter.intellij.pants.highlighting.PantsHighlightingIntegrationTest;
import com.twitter.intellij.pants.quickfix.AddPantsTargetDependencyFix;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class ResolveIntegrationTest extends PantsHighlightingIntegrationTest {


  public void testTestsResourcesCommonContentRoot() throws Throwable {
    doImport("intellij-integration/extras/");
    PsiClass psiClass = findClassAndAssert("org.pantsbuild.testproject.DummyTest");
    final Editor editor = createEditor(psiClass.getContainingFile().getVirtualFile());
    assertNotNull(editor);
    final HighlightInfo info = findInfo(doHighlighting(psiClass.getContainingFile(), editor), "Cannot resolve symbol 'Greeting'");
    assertNotNull(info);
    //final AddPantsTargetDependencyFix intention = findIntention(info, AddPantsTargetDependencyFix.class);
    //assertNotNull(intention);
    //int x = 5;
    //assertModules(
    //  "intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1"
    //);
    //
    //makeModules("intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1");
    //assertClassFileInModuleOutput(
    //  "org.pantsbuild.testproject.excludes1.Foo", "intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1"
    //);
  }
}
