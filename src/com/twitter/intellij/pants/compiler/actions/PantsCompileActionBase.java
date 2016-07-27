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
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PantsCompileActionBase is a an abstract action that implements basic compilation activities
 */
public abstract class PantsCompileActionBase extends AnAction implements DumbAware {

  public PantsCompileActionBase(String name) {
    super(name);
  }

  @Nullable
  public abstract Stream<PantsTargetAddress> getTargets(AnActionEvent e, @NotNull Project project);

  public abstract boolean doCleanAll();

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();

    if (project != null) {
      Stream<PantsTargetAddress> pantsTargets = getTargets(e, project);
      if (pantsTargets == null) {
        return;
      }

      Set<String> fullTargets = pantsTargets
        .map(PantsTargetAddress::toString)
        .collect(Collectors.toSet());

      PantsMakeBeforeRun runner = (PantsMakeBeforeRun) ExternalSystemBeforeRunTaskProvider.getProvider(project, PantsMakeBeforeRun.ID);
      ApplicationManager.getApplication().executeOnPooledThread(() -> runner.executeTask(project, fullTargets, doCleanAll()));
    } else {
      Notification notification = new Notification(
        PantsConstants.PANTS,
        PantsIcons.Icon,
        "Project not found",
        "Compile failed",
        null,
        NotificationType.ERROR,
        null
      );
      Notifications.Bus.notify(notification);
    }
  }
}
