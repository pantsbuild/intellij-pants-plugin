// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.execution.PantsExecuteTaskResult;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PantsTaskActionBase is an abstract action that executes Pants tasks on a stream of targets.
 */
public abstract class PantsTaskActionBase extends AnAction implements DumbAware {

  public PantsTaskActionBase(@NotNull final String name) {
    super(name);
  }

  @NotNull
  public abstract Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project);

  @NotNull
  public abstract PantsExecuteTaskResult execute(@NotNull PantsMakeBeforeRun runner,
                                                 @NotNull Project project,
                                                 @NotNull Set<String> targetAddresses);

  @Override
  public void actionPerformed(@Nullable AnActionEvent e) {
    if (e == null) {
      // TODO: signal if null event provided?
      return;
    }

    Project project = e.getProject();

    if (project == null) {
      // TODO: signal on null project?
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

    Set<String> fullTargets = this.getTargets(e, project).collect(Collectors.toSet());
    PantsMakeBeforeRun runner = (PantsMakeBeforeRun) ExternalSystemBeforeRunTaskProvider.getProvider(project, PantsMakeBeforeRun.ID);
    ApplicationManager.getApplication().executeOnPooledThread(() -> execute(runner, project, fullTargets));
  }
}
