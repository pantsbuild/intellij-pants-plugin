// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.notification.EventLog;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.file.FileChangeTracker;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import javax.swing.event.HyperlinkEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OSSRefreshPromptIntegrationTest extends OSSPantsIntegrationTest {

  private static List<Notification> findAllExistingRefreshNotification(Project project) {
    ArrayList<Notification> notifications = EventLog.getLogModel(project).getNotifications();
    return notifications.stream().filter(s -> s.getContent().contains(FileChangeTracker.REFRESH_PANTS_PROJECT_DISPLAY))
      .collect(Collectors.toList());
  }

  /**
   * Modifying a BUILD file in project should not trigger refresh prompt.
   */
  public void testRefreshPrompt() throws Throwable {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        doImport("examples/tests/java/org/pantsbuild/example/useproto");

        // Find a BUILD file in project.
        FileEditorManager.getInstance(myProject).openFile(firstMatchingVirtualFileInProject("BUILD"), true);
        Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();

        // Add a newline to the BUILD file.
        editor.getDocument().setText(editor.getDocument().getText() + "\n");

        // Save the BUILD file. Then the refresh notification should be triggered.
        FileDocumentManager.getInstance().saveAllDocuments();

        // Verify the notification is triggered.
        List<Notification> refreshNotifications = findAllExistingRefreshNotification(myProject);
        assertEquals(1, refreshNotifications.size());
        Notification notification = refreshNotifications.get(0);
        assertEquals(PantsBundle.message("pants.project.build.files.changed"), notification.getTitle());

        /*
         Currently there is no good to way to prove a project has been refreshed, so we introducued some changes to .proto file.
         Since the changes can only be reflected via project refresh, the updated java file from protobuf
         modification can be used to prove project refresh has happened.
        */

        // Open 'Distances.java' in editor and make sure it only contains `getNumber` and not `getNewDummyNumber`.
        FileEditorManager.getInstance(myProject).openFile(firstMatchingVirtualFileInProject("Distances.java"), true);
        Editor editor2 = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
        assertContainsSubstring(Collections.singletonList(editor2.getDocument().getText()), "getNumber()");
        assertNotContainsSubstring(Collections.singletonList(editor2.getDocument().getText()), "getNewDummyNumber()");

        // Find distances.proto, and replaces it with the new text which adds the line "required int64 new_dummy_number = 3"
        Document distanceDotProtoDoc = FileDocumentManager.getInstance().getDocument(firstMatchingVirtualFileInProject("distances.proto"));
        Document newDistanceProtoDoc = getTestData("testData/protobuf/distances.proto");
        distanceDotProtoDoc.setText(newDistanceProtoDoc.getText());

        // Click the hyperlink to trigger project refresh and make sure `getNewDummyNumber()` is loaded into the Editor.
        final URL url = null;
        HyperlinkEvent event = new HyperlinkEvent(notification, HyperlinkEvent.EventType.ACTIVATED, url, FileChangeTracker.HREF_REFRESH);
        NotificationListener listener = notification.getListener();
        assertNotNull("Notification should have a listener set, but does not", listener);
        listener.hyperlinkUpdate(notification, event);

        assertContainsSubstring(Collections.singletonList(editor2.getDocument().getText()), "getNumber()");
        assertContainsSubstring(Collections.singletonList(editor2.getDocument().getText()), "getNewDummyNumber()");
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
        FileEditorManager.getInstance(myProject).openFile(firstMatchingVirtualFileInProject("UseProtoTest.java"), true);
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

  public void testNotificationQuantity() throws Throwable {
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        doImport("examples/tests/java/org/pantsbuild/example/useproto");
        // Find a BUILD file in project.
        FileEditorManager.getInstance(myProject).openFile(firstMatchingVirtualFileInProject("BUILD"), true);
        Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();

        // Save the file multiple times and make sure there is only one active refresh notification
        for (int i = 0; i < 2; i++) {
          saveFileAndAssertRefreshNotification(editor);
        }

        // Expire the existing one
        EventLog.getLogModel(myProject).getNotifications().forEach(Notification::expire);
        // Make sure there is no active one left
        assertEquals(0, EventLog.getLogModel(myProject).getNotifications().size());
        saveFileAndAssertRefreshNotification(editor);
      }

      /**
       * Change the text file in an editor and make sure there is only one refresh notification.
       * @param editor an editor containing an opened file.
       */
      private void saveFileAndAssertRefreshNotification(Editor editor) {
        // Add a newline to the BUILD file.
        editor.getDocument().setText(editor.getDocument().getText() + "\n");
        // Save the BUILD file. Then the refresh notification should be triggered.
        FileDocumentManager.getInstance().saveAllDocuments();
        // Verify the notification is triggered.
        List<Notification> notifications = findAllExistingRefreshNotification(myProject);
        assertEquals(
          String.format("Project should only have 1 refresh notification, but has %s", notifications.size()),
          1, notifications.size()
        );
      }
    });
  }

  @Override
  public void tearDown() throws Exception {
    gitResetRepoCleanExampleDistDir();
    super.tearDown();
  }
}
