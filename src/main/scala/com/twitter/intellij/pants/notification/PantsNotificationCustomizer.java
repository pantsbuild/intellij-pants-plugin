// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.notification;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationExtension;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.PantsExecutionException;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

public class PantsNotificationCustomizer implements ExternalSystemNotificationExtension {
  @NotNull
  @Override
  public ProjectSystemId getTargetExternalSystemId() {
    return PantsConstants.SYSTEM_ID;
  }

  @Override
  public void customize(@NotNull NotificationData notificationData, @NotNull Project project, Throwable error) {
    if (error instanceof PantsExecutionException) {
      customizeExecutionException(notificationData, (PantsExecutionException)error);
    }
  }

  public void customizeExecutionException(@NotNull NotificationData notificationData, @NotNull PantsExecutionException ex) {
    if (ex.isTerminated()) {
      notificationData.setBalloonNotification(true);
      notificationData.setMessage(PantsBundle.message("pants.command.terminated"));
    } else {
      notificationData.setMessage(ex.getExecutionDetails());
    }
  }
}
