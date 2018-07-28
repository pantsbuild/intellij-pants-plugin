// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;

import java.util.Collections;

public class OSSFileSyncIntegrationTest extends OSSPantsIntegrationTest {

  public void testFileSync() throws Throwable {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        doImport("examples/tests/java/org/pantsbuild/example/useproto");
        /**
         * Open 'Distances.java' in editor and make sure it only contains `getNumber` and not `getNewDummyNumber`.
         */
        FileEditorManager.getInstance(myProject).openFile(searchForVirtualFileInProject("Distances.java"), true);
        Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();

        assertContainsSubstring(Collections.singletonList(editor.getDocument().getText()), "getNumber()");
        assertNotContainsSubstring(Collections.singletonList(editor.getDocument().getText()), "getNewDummyNumber()");

        /**
         * Find distances.proto, and replaces it with the new text which adds the line "required int64 new_dummy_number = 3"
         */
        Document distanceDotProtoDoc = getDocumentFileInProject("distances.proto");
        Document newDistanceProtoDoc = getTestData("testData/protobuf/distances.proto");
        distanceDotProtoDoc.setText(newDistanceProtoDoc.getText());

        /**
         * Refresh projects and make sure `getNewDummyNumber()` is loaded into the Editor.
         */
        PantsUtil.refreshAllProjects(myProject);
        assertContainsSubstring(Collections.singletonList(editor.getDocument().getText()), "getNumber()");
        assertContainsSubstring(Collections.singletonList(editor.getDocument().getText()), "getNewDummyNumber()");
      }
    });
  }

  @Override
  public void tearDown() throws Exception {
    gitResetRepoCleanExampleDistDir();
    super.tearDown();
  }
}
