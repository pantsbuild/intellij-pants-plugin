// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.notification.EventLog;
import com.intellij.notification.Notification;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class OSSRefreshPromptIntegrationTest extends OSSPantsIntegrationTest {

  public void testRefreshPrompt() throws Throwable {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        doImport("examples/tests/java/org/pantsbuild/example/useproto");
        // Find a BUILD file in project.
        FileEditorManager.getInstance(myProject).openFile(searchForVirtualFileInProject("BUILD"), true);
        Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        // Add a newline to the BUILD file.
        editor.getDocument().setText(editor.getDocument().getText() + "\n");
        // Save the BUILD file. Then the refresh notification should be triggered.
        FileDocumentManager.getInstance().saveAllDocuments();
        // Verify the notification is triggered.
        ArrayList<Notification> notifications = EventLog.getLogModel(myProject).getNotifications();
        int notificationSize = notifications.size();
        assertTrue(notificationSize > 0);
        assertEquals(PantsBundle.message("pants.project.build.files.changed"), notifications.get(notificationSize - 1).getTitle());
      }
    });
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

  @Override
  public void tearDown() throws Exception {
    gitCleanExampleDir();
    super.tearDown();
  }
}
