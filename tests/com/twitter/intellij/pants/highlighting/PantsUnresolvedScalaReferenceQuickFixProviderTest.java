// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.highlighting;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.quickfix.AddPantsTargetDependencyFix;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PantsUnresolvedScalaReferenceQuickFixProviderTest extends PantsHighlightingIntegrationTest {
  @Override
  protected String[] getRequiredPluginIds() {
    return ArrayUtil.append(super.getRequiredPluginIds(), "PythonCore");
  }

  public void testMissingDepsWhiteList() throws Throwable {
    doImport("intellij-integration/src/scala/org/pantsbuild/testproject/missingdepswhitelist");

    assertModules(
      "intellij-integration_src_scala_org_pantsbuild_testproject_missingdepswhitelist_missingdepswhitelist",
      "intellij-integration_src_scala_org_pantsbuild_testproject_missingdepswhitelist2_missingdepswhitelist2",
      "testprojects_src_java_org_pantsbuild_testproject_publish_hello_greet_greet"
    );

    final PsiClass psiClass = findClassAndAssert("org.pantsbuild.testproject.missingdepswhitelist2.MissingDepsWhitelist2");
    final Editor editor = createEditor(psiClass.getContainingFile().getVirtualFile());
    assertNotNull(editor);
    final HighlightInfo info = findInfo(doHighlighting(psiClass.getContainingFile(), editor), "Cannot resolve symbol Greeting");
    assertNotNull(info);
    final AddPantsTargetDependencyFix intention = findIntention(info, AddPantsTargetDependencyFix.class);
    assertNotNull(intention);

    assertModuleModuleDeps("intellij-integration_src_scala_org_pantsbuild_testproject_missingdepswhitelist2_missingdepswhitelist2");

    testIntention(intention, editor, psiClass);
  }

  private void testIntention(
    @NotNull final AddPantsTargetDependencyFix intention,
    @NotNull final Editor editor,
    @NotNull final PsiClass psiClass
  ) throws Exception {
    assertCompilationFailed("intellij-integration_src_scala_org_pantsbuild_testproject_missingdepswhitelist2_missingdepswhitelist2");

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
      "intellij-integration_src_scala_org_pantsbuild_testproject_missingdepswhitelist2_missingdepswhitelist2",
      "testprojects_src_java_org_pantsbuild_testproject_publish_hello_greet_greet"
    );

    makeProject();

    final Module module = getModule("intellij-integration_src_scala_org_pantsbuild_testproject_missingdepswhitelist2_missingdepswhitelist2");
    final List<String> messages =
      ContainerUtil.map(
        compileAndGetMessages(module),
        new Function<CompilerMessage, String>() {
          @Override
          public String fun(CompilerMessage message) {
            return message.getMessage();
          }
        }
      );
    assertContain(messages, "pants: No changes to compile.");
  }
}
