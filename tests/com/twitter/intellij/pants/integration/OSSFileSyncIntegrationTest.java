// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.testFramework.PantsTestUtils;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

public class OSSFileSyncIntegrationTest extends OSSPantsIntegrationTest {

  private final static boolean readOnly = false;

  public OSSFileSyncIntegrationTest() {
    super(readOnly);
  }

  public void testFileSync() throws Throwable {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        doImport("examples/tests/java/org/pantsbuild/example/useproto");
        /**
         * Open 'Distances.java' in editor and make sure it only contains `getNumber` and not `getNewDummyNumber`.
         */
        FileEditorManager.getInstance(myProject).openFile(getVirtualFileInProject("Distances.java"), true);
        Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();

        assertContainsSubstring(Collections.singletonList(editor.getDocument().getText()), "getNumber()");
        assertNotContainsSubstring(Collections.singletonList(editor.getDocument().getText()), "getNewDummyNumber()");

        /**
         * Find distances.proto, and replaces it with the new text which adds the line "required int64 new_dummy_number = 3"
         */
        Document distanceDotProtoDoc = getFileInProjectByName("distances.proto");
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

  @NotNull
  private Document getTestData(String testDataPath) {
    File newProto = PantsTestUtils.findTestPath(testDataPath);
    VirtualFile newDistanceDotProto = VirtualFileManager.getInstance().findFileByUrl("file://" + newProto.getPath());
    assertNotNull(newDistanceDotProto);
    Document newDistanceProtoDoc = FileDocumentManager.getInstance().getDocument(newDistanceDotProto);
    assertNotNull(newDistanceProtoDoc);
    return newDistanceProtoDoc;
  }

  /**
   * Find the document associated with the filename. Return the cached version first if available, otherwise actually go find it.
   */
  private Document getFileInProjectByName(String filename) {
    VirtualFile sourceFile = getVirtualFileInProject(filename);
    Document doc = FileDocumentManager.getInstance().getDocument(sourceFile);
    assertNotNull(String.format("%s not found.", filename), doc);
    return doc;
  }

  private VirtualFile getVirtualFileInProject(String filename) {
    Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(myProject, filename, GlobalSearchScope.allScope(myProject));
    assertEquals(1, files.size());
    return files.iterator().next();
  }
}
