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
import com.twitter.intellij.pants.settings.PantsSettings;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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

    // In CI we run all tests with both compiler options. See .travis.yml
    // But in this test we also checking the difference between compiler options.
    // We are checking that we can compile the project with Pants even with a missing dependency
    // and we do not compile project with Pants again if no source files were changed.
    if (PantsSettings.getInstance(myProject).isCompileWithIntellij()) {
      testIntentionIfCompilingWithIntelliJ(intention, editor, psiClass);
    } else {
      testIntentionIfCompilingWithPants(intention, editor, psiClass);
    }
  }

  private void testIntentionIfCompilingWithIntelliJ(
    @NotNull final AddPantsTargetDependencyFix intention,
    @NotNull final Editor editor,
    @NotNull final PsiClass psiClass
  ) throws Exception {
    assertCompilationFailed("intellij-integration_src_scala_com_pants_testproject_missingdepswhitelist2_missingdepswhitelist2");

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

    makeProject();
  }

  private void testIntentionIfCompilingWithPants(
    @NotNull final AddPantsTargetDependencyFix intention,
    @NotNull final Editor editor,
    @NotNull final PsiClass psiClass
  ) throws Exception {
    // we should be able to compile it even with a missing dependency
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

    makeProject();

    final Module module = getModule("intellij-integration_src_scala_com_pants_testproject_missingdepswhitelist2_missingdepswhitelist2");
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
