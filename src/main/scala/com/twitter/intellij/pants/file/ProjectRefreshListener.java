// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.file;

import com.intellij.notification.EventLog;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

import java.util.ArrayList;

import static com.twitter.intellij.pants.file.FileChangeTracker.HREF_REFRESH;

public final class ProjectRefreshListener extends NotificationListener.Adapter {
  public static final String NOTIFICATION_TITLE = PantsBundle.message("pants.project.build.files.changed");
  private static final String NOTIFICATION_BUTTON_TITLE = "Refresh Pants Project";

  private final Project project;

  private static boolean hasExistingRefreshNotification(Project project) {
    ArrayList<Notification> notifications = EventLog.getLogModel(project).getNotifications();
    return notifications.stream().anyMatch(s -> s.getTitle().equals(NOTIFICATION_TITLE));
  }

  /**
   * Template came from maven plugin:
   * https://github.com/JetBrains/intellij-community/blob/b5d046018b9a82fccd86bc9c1f1da2e28068440a/plugins/maven/src/main/java/org/jetbrains/idea/maven/utils/MavenImportNotifier.java#L92-L108
   */
  static void notify(Project project) {
    if(hasExistingRefreshNotification(project)){
      return;
    }

    Notification notification = new Notification(
      PantsConstants.PANTS,
      NOTIFICATION_TITLE,
      "<a href='refresh'>" + NOTIFICATION_BUTTON_TITLE + "</a> ",
      NotificationType.INFORMATION,
      new ProjectRefreshListener(project)
    );

    notification.notify(project);
  }

  private ProjectRefreshListener(Project project) {
    this.project = project;
  }

  @Override
  protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
    if (HREF_REFRESH.equals(event.getDescription())) {
      PantsUtil.refreshAllProjects(project);
    }

    notification.expire();
  }
}
