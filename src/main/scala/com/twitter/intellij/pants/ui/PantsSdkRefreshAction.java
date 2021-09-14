// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.twitter.intellij.pants.util.PantsSdkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsSdkRefreshAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      Messages.showInfoMessage("Project not found.", "Error");
      return;
    }

    ProgressManager.getInstance().run(new RefreshJdkTask(project));
  }

  private static class RefreshJdkTask extends Task.Backgroundable {
    RefreshJdkTask(@Nullable Project project) {
      super(project, "Refreshing SDK", false);
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
      Sdk sdk = ProjectRootManager.getInstance(myProject).getProjectSdk();
      PantsSdkUtil.refreshJdk(myProject, sdk, null);
    }
  }
}
