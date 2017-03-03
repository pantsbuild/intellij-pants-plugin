// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.highlighting;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.quickfix.AddPantsTargetDependencyFix;

public class PantsUnresolvedJavaReferenceQuickFixProviderTest extends PantsHighlightingIntegrationTest {
  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "PythonCore");
  }

  public void testMissingDepsWhiteList() throws Throwable {
    doImport("testprojects/src/java/org/pantsbuild/testproject/missingdepswhitelist");

    assertFirstSourcePartyModules(
      "testprojects_src_java_org_pantsbuild_testproject_missingdepswhitelist_missingdepswhitelist",
      "testprojects_src_java_org_pantsbuild_testproject_missingdepswhitelist2_missingdepswhitelist2",
      "testprojects_src_java_org_pantsbuild_testproject_publish_hello_greet_greet"
    );

    final PsiClass psiClass = findClassAndAssert("org.pantsbuild.testproject.missingdepswhitelist2.MissingDepsWhitelist2");
    final Editor editor = createEditor(psiClass.getContainingFile().getVirtualFile());
    assertNotNull(editor);
    final HighlightInfo info = findInfo(doHighlighting(psiClass.getContainingFile(), editor), "Cannot resolve symbol 'Greeting'");
    assertNotNull(info);
    final AddPantsTargetDependencyFix intention = findIntention(info, AddPantsTargetDependencyFix.class);
    assertNotNull(intention);

    assertModuleModuleDeps("testprojects_src_java_org_pantsbuild_testproject_missingdepswhitelist2_missingdepswhitelist2");

    WriteCommandAction.runWriteCommandAction(
      myProject,
      new Runnable() {
        @Override
        public void run() {
          intention.invoke(myProject, editor, psiClass.getContainingFile());
        }
      }
    );

    assertModuleModuleDeps(
      "testprojects_src_java_org_pantsbuild_testproject_missingdepswhitelist2_missingdepswhitelist2",
      "testprojects_src_java_org_pantsbuild_testproject_publish_hello_greet_greet"
    );

    assertPantsCompileExecutesAndSucceeds(
      pantsCompileModule("testprojects_src_java_org_pantsbuild_testproject_missingdepswhitelist2_missingdepswhitelist2"));

  }
}
