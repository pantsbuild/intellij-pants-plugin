// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;
import org.intellij.images.thumbnail.ThumbnailManager;
import org.intellij.images.thumbnail.ThumbnailView;

/**
 * PantsCompileAllTargets is a UI action that, when in a project, compiles all targets in the project
 */
public class PantsCompileTarget extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = (VirtualFile)e.getData(CommonDataKeys.VIRTUAL_FILE);
    System.out.println("got here boi");
  }
  // TODO: test this joint
  /*public void update(AnActionEvent e) {
    super.update(e);
    VirtualFile file = (VirtualFile)e.getData(CommonDataKeys.VIRTUAL_FILE);
    boolean isEnabled = file != null && file.isDirectory();
    if(e.getPlace().equals("ProjectViewPopup")) {
      e.getPresentation().setVisible(isEnabled);
    } else {
      e.getPresentation().setEnabled(isEnabled);
    }

  }*/
}
