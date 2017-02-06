// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.notification.EventLog;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
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

import javax.swing.event.HyperlinkEvent;
import java.util.ArrayList;
import java.util.Collection;

public class OSSRefreshPromptIntegrationTest extends OSSPantsIntegrationTest {

  /**
   * Modifying a BUILD file in project should not trigger refresh prompt.
   */
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
        Notification notification = notifications.get(notificationSize - 1);
        assertEquals(PantsBundle.message("pants.project.build.files.changed"), notification.getTitle());

        NotificationListener listener = notification.getListener();
        assertNotNull("Notification should have a listener set, but does not", listener);
        HyperlinkEvent event = new HyperlinkEvent(notification, HyperlinkEvent.EventType.ACTIVATED, null, notification.getContent());
        listener.hyperlinkUpdate(notification, event);
      }
    });
  }

  /**
   * Modifying a non BUILD file in project should not trigger refresh prompt.
   */
  public void testNoRefreshPrompt() throws Throwable {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        doImport("examples/tests/java/org/pantsbuild/example/useproto");
        // Find a BUILD file in project.
        FileEditorManager.getInstance(myProject).openFile(searchForVirtualFileInProject("UseProtoTest.java"), true);
        Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        // Add a newline to the BUILD file.
        editor.getDocument().setText(editor.getDocument().getText() + "\n");
        // Save the BUILD file. Then the refresh notification should be triggered.
        FileDocumentManager.getInstance().saveAllDocuments();
        // Verify the notification is triggered.
        ArrayList<Notification> notifications = EventLog.getLogModel(myProject).getNotifications();
        assertEquals("There should not be any notifications, but there is", 0, notifications.size());
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
    gitResetRepoCleanExampleDistDir();
    super.tearDown();
  }
}
