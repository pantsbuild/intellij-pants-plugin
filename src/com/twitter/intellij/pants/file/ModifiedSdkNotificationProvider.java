// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.file;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.twitter.intellij.pants.util.PantsSdkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>This notifies the user that the sdk defined for the opened file was modified.
 * The user can then either refresh the sdk or ignore the notification.</p>
 *
 * <p>Modifications are detected by other modules (see @{@link FileChangeTracker})
 * and communicated via @{@link ModifiedSdkNotificationProvider#notifySdkModified(Project)}</p>
 */
public final class ModifiedSdkNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
  public static final Key<EditorNotificationPanel> KEY = Key.create("Refresh stale SDK");
  private static final Set<Project> PROJECTS_WITH_MODIFIED_SDK = ConcurrentHashMap.newKeySet();

  static void notifySdkModified(Project project) {
    boolean isNewModification = PROJECTS_WITH_MODIFIED_SDK.add(project);
    if (isNewModification) {
      updateNotification(project);
    }
  }

  private static void updateNotification(Project project) {
    EditorNotifications.getInstance(project).updateAllNotifications();
  }

  private static void removeNotification(Project project) {
    PROJECTS_WITH_MODIFIED_SDK.remove(project);
    updateNotification(project);
  }

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(
    @NotNull VirtualFile file,
    @NotNull FileEditor fileEditor,
    @NotNull Project project
  ) {
    if (!PROJECTS_WITH_MODIFIED_SDK.contains(project)) {
      return null;
    }

    try {
      Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();

      if (sdk == null) {
        // if the notification cannot be shown now, we have to unblock
        // the project for future modification notifications
        PROJECTS_WITH_MODIFIED_SDK.remove(project);
        return null;
      }
      else {
        return new RefreshSdkNotificationPanel(project, sdk);
      }
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      // just throwing the exception would block this notification for the entire project
      // (new modification notifications would not be picked up)
      PROJECTS_WITH_MODIFIED_SDK.remove(project);
      throw e;
    }
  }

  private static class RefreshSdkNotificationPanel extends EditorNotificationPanel {
    private final AtomicBoolean clicked = new AtomicBoolean(false);
    private final Project project;
    private final Sdk sdk;

    RefreshSdkNotificationPanel(Project project, Sdk sdk) {
      this.project = project;
      this.sdk = sdk;
      setText("SDK modified: (" + sdk.getName() + ")");
      createActionLabel("Refresh SDK", this::refreshSdk);
      createActionLabel("Ignore", this::ignore);
    }

    private void refreshSdk() {
      if (clicked.compareAndSet(false, true)) {
        try {
          PantsSdkUtil.updateJdk(project, sdk);
        }
        finally {
          removeNotification(project);
        }
      }
    }

    private void ignore() {
      if (clicked.compareAndSet(false, true)) {
        removeNotification(project);
      }
    }
  }
}
