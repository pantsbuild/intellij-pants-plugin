// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.Icon;

/**
 * PantsOverrideAction is a wrapper action that toggles active actions on Pants or non-Pants projects
 */
public class PantsOverrideAction extends AnAction implements DumbAware {

  private final AnAction primaryPantsAction;
  private final AnAction secondaryIdeaAction;
  private final Icon secondaryIdeaIcon;
  @TestOnly
  private boolean pantsActive;

  public PantsOverrideAction(String oldName, @NotNull AnAction pantsAction, @Nullable AnAction ideaAction, Icon icon) {
    super(oldName);
    primaryPantsAction = pantsAction;
    secondaryIdeaAction = ideaAction;
    secondaryIdeaIcon = icon;
  }

  public PantsOverrideAction(String oldName, @NotNull AnAction pantsAction, @Nullable AnAction ideaAction) {
    this(oldName, pantsAction, ideaAction, null);
  }

  public PantsOverrideAction(@NotNull AnAction pantsPrimary, @Nullable  AnAction ideaSecondary) {
    primaryPantsAction = pantsPrimary;
    secondaryIdeaAction = ideaSecondary;
    secondaryIdeaIcon = null;
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
      if (secondaryIdeaIcon != null) {
        event.getPresentation().setIcon(secondaryIdeaIcon);
      }
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

  @Override
  @TestOnly
  public String toString() {
    String activeOverride = pantsActive ? " actively" : "";
    return
      primaryPantsAction.getClass().getSimpleName() + activeOverride +  " overriding " +
      (secondaryIdeaAction == null ? "NullAction" : secondaryIdeaAction.getClass().getSimpleName());
  }
}