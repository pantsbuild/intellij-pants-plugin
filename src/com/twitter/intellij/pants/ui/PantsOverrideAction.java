// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * PantsOverrideAction is a wrapper action that toggles active actions on Pants or non-Pants projects
 */
public  class PantsOverrideAction extends AnAction implements DumbAware {

  private AnAction secondaryIdeaAction;
  private AnAction primaryPantsAction;
  private boolean pantsActive;

  public PantsOverrideAction(String actionId, @NotNull AnAction pantsAction) {
    this(pantsAction, getAction(actionId));
  }

  public PantsOverrideAction(String actionId, String oldName, @NotNull AnAction pantsAction) {
    super(oldName);
    primaryPantsAction = pantsAction;
    secondaryIdeaAction = getAction(actionId);
  }

  public PantsOverrideAction(AnAction pantsPrimary, AnAction ideaSecondary) {
    primaryPantsAction = pantsPrimary;
    secondaryIdeaAction = ideaSecondary;
  }

  public static PantsOverrideAction createDisabledEmptyAction(String actionId) {
    return new PantsOverrideAction(actionId, new PantsShieldAction(getAction(actionId)));
  }

  private boolean isPantsProject(AnActionEvent event) {
    if (event == null) {
      return false;
    }
    Project project = event.getProject();
    pantsActive = project != null && PantsUtil.isPantsProject(project);
    return pantsActive;
  }

  @Override
  public void update(AnActionEvent event) {
    if (secondaryIdeaAction != null) {
      secondaryIdeaAction.update(event);
    }
    if (isPantsProject(event)) {
      event.getPresentation().setIcon(PantsIcons.Icon);
      primaryPantsAction.update(event);
    }
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    if (isPantsProject(event)) {
      primaryPantsAction.actionPerformed(event);
    }
    else if (secondaryIdeaAction != null) {
      secondaryIdeaAction.actionPerformed(event);
    }
  }

  private static AnAction getAction(@NotNull String actionId) {
    return ActionManager.getInstance().getAction(actionId);
  }

  @Override
  @TestOnly
  public String toString() {
    String activeOverride = pantsActive ? " actively" : "";
    return
      primaryPantsAction.getClass().getSimpleName() + activeOverride +  " overriding " +
      (secondaryIdeaAction == null ? "NullAction" : secondaryIdeaAction.getClass().getSimpleName());
  }
}