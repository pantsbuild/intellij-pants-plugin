// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TargetFileCompletionIntegrationTest extends OSSPantsIntegrationTest {

  public void testScalaLibCompletions() throws IOException {
    String toComplete = "scala_lib";
    String[] expected = {
      "scala_library(",
      "scala_js_library("
    };
    completionTest(toComplete, expected);
  }

  public void testJavaLibCompletions() throws IOException {
    String toComplete = "java";
    String[] expected = {
      "java_library(",
      "java_agent(",
      "javac_plugin(",
      "examples/src/java/org/pantsbuild/example/hello/greet"
    };

    completionTest(toComplete, expected);
  }

  private void completionTest(String stringToComplete, String[] expected) throws IOException {
    // should be only tested with pants versions above 1.24.0
    if (PantsUtil.isCompatiblePantsVersion(myProjectRoot.getPath(), "1.24.0")) {
      String helloProjectPath = "examples/src/scala/org/pantsbuild/example/hello/";
      doImport(helloProjectPath);
      VirtualFile vfile = myProjectRoot.findFileByRelativePath(helloProjectPath + "BUILD");
      assertNotNull(vfile);

      Document doc = FileDocumentManager.getInstance().getDocument(vfile);
      write(doc, "\n\n" + stringToComplete);
      PsiFile build = PsiManager.getInstance(myProject).findFile(vfile);
      String text = doc.getText();
      assertNotNull(text);
      int offset = text.indexOf(stringToComplete);

      final PsiReference reference = build.findReferenceAt(offset);
      assertNotNull("no reference", reference);

      Editor editor = EditorFactory.getInstance().createEditor(doc, myProject);
      editor.getCaretModel().moveToOffset(offset + stringToComplete.length());

      new CodeCompletionHandlerBase(CompletionType.BASIC, false, false, true).invokeCompletion(myProject, editor);

      List<String> actual = LookupManager
        .getActiveLookup(editor)
        .getItems()
        .stream()
        .map(LookupElement::getLookupString)
        .collect(Collectors.toList());

      Arrays.stream(expected).forEach(str -> assertContain(actual, str));
      EditorFactory.getInstance().releaseEditor(editor);
    }
  }

  private void write(Document doc, String addition) {
    WriteCommandAction.runWriteCommandAction(
      myProject,
      () -> doc.insertString(doc.getTextLength(), addition)
    );
  }
}
