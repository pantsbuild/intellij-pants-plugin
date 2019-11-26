// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.twitter.intellij.pants.util.PantsSdkUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class PantsSdkRefreshAction extends AnAction implements DumbAware {
  private final AtomicBoolean running = new AtomicBoolean(false);

  @Override
  public void actionPerformed(AnActionEvent e) {
    if (running.compareAndSet(false, true)) {
      try {
        Project project = e.getProject();
        if (project == null) {
          Messages.showInfoMessage("Project not found.", "Error");
          return;
        }
        Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
        PantsSdkUtil.updateJdk(project, sdk, null);
      }
      finally {
        running.set(false);
      }
    }
  }
}
