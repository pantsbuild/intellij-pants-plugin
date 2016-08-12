// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.notification;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;


public class PantsNotificationWrapper {
  private static final LinkedList<String> log = new LinkedList<>();

  public static LinkedList<String> getLog() {
    return log;
  }

  public static void notify(@NotNull final Notification notification) {
    log.add(notification.getContent());
    if (log.size() > 200) {
      log.removeFirst();
    }
    Notifications.Bus.notify(notification);
  }
}
