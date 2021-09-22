// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.util.ExternalProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.bsp.BSP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AmendAction extends AnAction {
  Logger logger = Logger.getInstance(AmendAction.class);

  public static final DataKey<AmendData> amendDataKey = DataKey.create("amendData");

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    try {
      AmendData amendData = event.getData(amendDataKey);
      refreshProjectsWithNewTargetsList(event.getProject(), amendData.getTargetSpecs(), amendData.getPantsBspData());
    }
    catch (Throwable e) {
      logger.error(e);
    }
  }

  private void refreshProjectsWithNewTargetsList(
    @NotNull Project project,
    Collection<String> newTargets,
    PantsBspData basePath
  ) {
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Amending", false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          AmendService amendService = ServiceManager.getService(project, AmendService.class);
          Optional<CompletableFuture<Void>> amendProcess =
            amendService.amendAll(basePath, new ArrayList<>(newTargets), project);
          if(!amendProcess.isPresent()){
            Messages.showInfoMessage(project,
                                     PantsBundle.message("pants.message.amend.in.progress"),
                                     PantsBundle.message("pants.message.amend.in.progress")
            );
          } else {
            amendProcess.get().join();
            ExternalProjectUtil.refresh(project, BSP.ProjectSystemId());
          }
        }
        catch (Throwable e) {
          logger.error(e);
        }
      }
    });
  }
}
