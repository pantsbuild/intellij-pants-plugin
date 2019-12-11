// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.Lookup;
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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TargetFileCompletionIntegrationTest extends OSSPantsIntegrationTest {

  final String CURSOR = "<CURSOR>";

  public void testScalaLibCompletions() {
    String toComplete = "scala_lib" + CURSOR;
    String[] expected = {
      "scala_library(",
      "scala_js_library("
    };
    completionTest(toComplete, expected);
  }

  public void testScalaNoCompletions() {
    String toComplete = "scala_library(sca" + CURSOR + ")";
    String[] expected = {};
    completionTest(toComplete, expected);
  }

  public void testDependencies() {
    String toComplete = "scala_library(    dependencies = [\"example" + CURSOR + "\"])";
    String[] expected = {
      "examples",
      "examples/src/resources/org/pantsbuild/example/jaxb",
      "examples/src/scala/org/pantsbuild/example/hello/welcome",
      "examples/src/resources/org/pantsbuild/example/hello",
      "examples/src/java/org/pantsbuild/example/hello/greet",
      "examples/src/resources/org/pantsbuild/example/names",
      "examples/src/resources/org/pantsbuild/example",
      "examples/src/scala/org/pantsbuild/example/hello/exe",
      "examples/src/scala/org/pantsbuild/example/hello"
    };
    completionTest(toComplete, expected);
  }

  public void testNoDependencies() {
    String toComplete = "scala_library(name=\"example" + CURSOR + "\")";
    String[] expected = {};
    completionTest(toComplete, expected);
  }


  public void testJavaLibCompletions() {
    String toComplete = "java" + CURSOR;
    String[] expected = {
      "java_library(",
      "java_agent(",
      "javac_plugin(",
      "java_antlr_library(",
      "java_avro_library(",
      "java_protobuf_library(",
      "java_ragel_library(",
      "java_thrift_library(",
      "java_thrifty_library(",
      "java_wire_library("
    };

    completionTest(toComplete, expected);
  }

  private void completionTest(String stringToComplete, String[] expected) {
    String fullStringToComplete = "\n\n" + stringToComplete;
    // should be only tested with pants versions above 1.24.0
    if (PantsUtil.isCompatiblePantsVersion(myProjectRoot.getPath(), "1.24.0")) {
      String helloProjectPath = "examples/src/scala/org/pantsbuild/example/hello/";
      doImport(helloProjectPath);
      VirtualFile vfile = myProjectRoot.findFileByRelativePath(helloProjectPath + "BUILD");
      assertNotNull(vfile);

      Document doc = FileDocumentManager.getInstance().getDocument(vfile);
      int offset = doc.getText().length() + fullStringToComplete.indexOf(CURSOR);
      write(doc, fullStringToComplete.replace(CURSOR, ""));
      String text = doc.getText();
      assertNotNull(text);

      Editor editor = EditorFactory.getInstance().createEditor(doc, myProject);
      editor.getCaretModel().moveToOffset(offset);

      new CodeCompletionHandlerBase(CompletionType.BASIC, false, false, true).invokeCompletion(myProject, editor);

      List<LookupElement> elements =
        Optional.ofNullable(
          LookupManager
            .getActiveLookup(editor)
        ).map(Lookup::getItems).orElse(new LinkedList<>());

      List<String> actual = elements.stream()
        .map(LookupElement::getLookupString)
        .collect(Collectors.toList());

      assertSameElements(actual, expected);
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
