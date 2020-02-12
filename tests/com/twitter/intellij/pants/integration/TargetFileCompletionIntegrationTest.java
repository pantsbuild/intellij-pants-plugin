// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.FileBasedIndex;
import com.twitter.intellij.pants.index.PantsAddressesIndex;
import com.twitter.intellij.pants.index.PantsTargetIndex;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsConstants;
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
      "examples/src/scala/org/pantsbuild/example/hello:hello",
      "examples/src/scala/org/pantsbuild/example/hello/exe:exe",
      "examples/src/scala/org/pantsbuild/example/hello/welcome:welcome",
      "examples/src/java/org/pantsbuild/example/hello/greet:greet",
      "examples/src/resources/org/pantsbuild/example/hello:hello",
      "examples/src/resources/org/pantsbuild/example:example",
      "examples/src/resources/org/pantsbuild/example/jaxb:jaxb",
      "examples/src/resources/org/pantsbuild/example/names:names",
      "examples/src/resources/org/pantsbuild/example:hello_directory",
      "examples/src/resources/org/pantsbuild/example:jaxb_directory",
      "examples/src/resources/org/pantsbuild/example:names_directory",
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

  private void invalidateCaches() {
    PropertiesComponent.getInstance().unsetValue(PantsConstants.PANTS_AVAILABLE_TARGETS_KEY);

    // NOTE: FileBasedIndex.invalidateCaches method does not clear the pants addresses index
    FileBasedIndex.getInstance().requestRebuild(PantsAddressesIndex.NAME);
    FileBasedIndex.getInstance().requestRebuild(PantsTargetIndex.NAME);
  }

  private void completionTest(String stringToComplete, String[] expected) {
    String fullStringToComplete = "\n\n" + stringToComplete;
    // should be only tested with pants versions above 1.24.0
    if (PantsUtil.isCompatiblePantsVersion(myProjectRoot.getPath(), "1.24.0")) {
      invalidateCaches();

      String helloProjectPath = "examples/src/scala/org/pantsbuild/example/hello/";
      doImport(helloProjectPath);
      VirtualFile vfile = myProjectRoot.findFileByRelativePath(helloProjectPath + "BUILD");
      assertNotNull(vfile);

      Document doc = FileDocumentManager.getInstance().getDocument(vfile);

      String originalContent = doc.getText();

      int offset = doc.getText().length() + fullStringToComplete.indexOf(CURSOR);

      append(doc, fullStringToComplete.replace(CURSOR, ""));
      assertNotNull(doc.getText());

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

      WriteAction.runAndWait(() -> doc.setText(originalContent));

      assertSameElements(actual, expected);
      EditorFactory.getInstance().releaseEditor(editor);
    }
  }

  private void append(Document doc, String addition) {
    WriteCommandAction.runWriteCommandAction(
      myProject,
      () -> doc.insertString(doc.getTextLength(), addition)
    );
  }
}
