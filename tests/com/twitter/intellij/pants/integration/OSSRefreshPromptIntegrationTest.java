// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

public class OSSRefreshPromptIntegrationTest extends OSSPantsIntegrationTest {

  private final static boolean readOnly = false;

  public OSSRefreshPromptIntegrationTest() {
    super(readOnly);
  }

  public void testRefreshPrompt() throws Throwable {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        doImport("examples/tests/java/org/pantsbuild/example/useproto");
        /**
         * Open 'Distances.java' in editor and make sure it only contains `getNumber` and not `getNewDummyNumber`.
         */
        FileEditorManager.getInstance(myProject).openFile(searchForVirtualFileInProject("BUILD"), true);
        Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        editor.getDocument().setText(editor.getDocument().getText() + "\n");
        FileDocumentManager.getInstance().saveAllDocuments();

        int x = 5;
      }
    });
  }

  @NotNull
  private Document getTestData(String testDataPath) {
    File dataFile = PantsTestUtils.findTestPath(testDataPath);
    VirtualFile dataVirtualFile = VirtualFileManager.getInstance().findFileByUrl("file://" + dataFile.getPath());
    assertNotNull(dataVirtualFile);
    Document dataDocument = FileDocumentManager.getInstance().getDocument(dataVirtualFile);
    assertNotNull(dataDocument);
    return dataDocument;
  }

  /**
   * Find document in project by filename.
   */
  @NotNull
  private Document getDocumentFileInProject(String filename) {
    VirtualFile sourceFile = searchForVirtualFileInProject(filename);
    Document doc = FileDocumentManager.getInstance().getDocument(sourceFile);
    assertNotNull(String.format("%s not found.", filename), doc);
    return doc;
  }

  /**
   * Find VirtualFile in project by filename.
   */
  @NotNull
  private VirtualFile searchForVirtualFileInProject(String filename) {
    Collection<VirtualFile> files = FilenameIndex.getVirtualFilesByName(myProject, filename, GlobalSearchScope.allScope(myProject));
    assertTrue(String.format("Filename %s not found in project", filename), files.size() > 0);
    return files.iterator().next();
  }
}
