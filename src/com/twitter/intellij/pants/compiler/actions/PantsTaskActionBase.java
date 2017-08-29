// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PantsTaskActionBase is a an abstract action that implements basic compilation activities
 */
public abstract class PantsTaskActionBase extends AnAction implements DumbAware {

  public final IPantsGetTargets getTargets;
  public final IPantsExecuteTask executeTask;

  public PantsTaskActionBase(@NotNull final IPantsGetTargets getTargets,
                             @NotNull final IPantsExecuteTask executeTask,
                             @NotNull final String name) {
    super(name);
    this.getTargets = getTargets;
    this.executeTask = executeTask;
  }

  @Override
  public void actionPerformed(@Nullable AnActionEvent e) {
    if (e == null) {
      return;
    }

    Project project = e.getProject();

    if (project == null) {
      // TODO: signal if no project found?
      Notification notification = new Notification(
        PantsConstants.PANTS,
        PantsIcons.Icon,
        "Pants task failed",
        "Project not found",
        null,
        NotificationType.ERROR,
        null
      );
      Notifications.Bus.notify(notification);
      return;
    }

    Set<String> fullTargets = this.getTargets.apply(IPantsGetTargets.getFileForEvent(e), project).collect(Collectors.toSet());
    PantsMakeBeforeRun runner = (PantsMakeBeforeRun) ExternalSystemBeforeRunTaskProvider.getProvider(project, PantsMakeBeforeRun.ID);
    ApplicationManager.getApplication().executeOnPooledThread(() -> this.executeTask.apply(runner, project, fullTargets));
  }
}
