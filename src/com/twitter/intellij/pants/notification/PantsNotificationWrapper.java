// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.notification;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;


/**
 * Wrapper for intellij notification system, so we can go back and check what has been notified in tests.
 */
public class PantsNotificationWrapper {
  private static final LinkedList<String> log = new LinkedList<>();
  private static int LIMIT = 200;

  public static LinkedList<String> getLog() {
    return log;
  }

  public static void notify(@NotNull final Notification notification) {
    log.add(notification.getContent());
    if (log.size() > LIMIT) {
      log.removeFirst();
    }
    Notifications.Bus.notify(notification);
  }
}
