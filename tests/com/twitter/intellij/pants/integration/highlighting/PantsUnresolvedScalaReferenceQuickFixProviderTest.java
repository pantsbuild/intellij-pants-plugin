// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.highlighting;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.quickfix.AddPantsTargetDependencyFix;

public class PantsUnresolvedScalaReferenceQuickFixProviderTest extends PantsHighlightingIntegrationTest {
  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "PythonCore");
  }

  public void testMissingDepsWhiteList() throws Throwable {
    doImport("intellij-integration/src/scala/com/pants/testproject/missingdepswhitelist");

    assertModules(
      "intellij-integration_src_scala_com_pants_testproject_missingdepswhitelist_missingdepswhitelist",
      "intellij-integration_src_scala_com_pants_testproject_missingdepswhitelist2_missingdepswhitelist2",
      "testprojects_src_java_com_pants_testproject_publish_hello_greet_greet"
    );

    final PsiClass psiClass = findClass("com.pants.testproject.missingdepswhitelist2.MissingDepsWhitelist2");
    assertNotNull(psiClass);
    final Editor editor = createEditor(psiClass.getContainingFile().getVirtualFile());
    assertNotNull(editor);
    final HighlightInfo info = findInfo(doHighlighting(psiClass.getContainingFile(), editor), "Cannot resolve symbol Greeting");
    assertNotNull(info);
    final AddPantsTargetDependencyFix intention = findIntention(info, AddPantsTargetDependencyFix.class);
    assertNotNull(intention);

    assertModuleModuleDeps("intellij-integration_src_scala_com_pants_testproject_missingdepswhitelist2_missingdepswhitelist2");

    // we should also be able to compile it even with a missing dependency
    // because we are compiling via compile goal
    makeModules("intellij-integration_src_scala_com_pants_testproject_missingdepswhitelist2_missingdepswhitelist2");

    WriteCommandAction.Simple.runWriteCommandAction(
      myProject,
      new Runnable() {
        @Override
        public void run() {
          intention.invoke(myProject, editor, psiClass.getContainingFile());
        }
      }
    );

    assertModuleModuleDeps(
      "intellij-integration_src_scala_com_pants_testproject_missingdepswhitelist2_missingdepswhitelist2",
      "testprojects_src_java_com_pants_testproject_publish_hello_greet_greet"
    );

    makeModules("intellij-integration_src_scala_com_pants_testproject_missingdepswhitelist2_missingdepswhitelist2");
  }
}
