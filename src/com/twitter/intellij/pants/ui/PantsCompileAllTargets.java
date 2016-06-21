// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;

/**
 * PantsCompileAllTargets is a UI action that, when in a project, compiles all targets in the project
 */
public class PantsCompileAllTargets extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();

    if (project != null) {
      PantsMakeBeforeRun runner = new PantsMakeBeforeRun(e.getProject());
      runner.executeTask(e.getProject(), ModuleManager.getInstance(e.getProject()).getModules());
    } else {
      Notification notification = new Notification(PantsConstants.PANTS, PantsIcons.Icon, "Project not found", "Compile failed", null,
                                                   NotificationType.ERROR, null);
      Notifications.Bus.notify(notification);
    }
  }
}
