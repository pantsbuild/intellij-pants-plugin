// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.ExternalProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.bsp.BSP;

import java.util.ArrayList;
import java.util.Collection;

public class AmendAction extends AnAction {
  Logger logger = Logger.getInstance(AmendAction.class);

  public static final DataKey<AmendData> amendDataKey = DataKey.create("amendData");

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    try {
      AmendData amendData = event.getData(amendDataKey);
      refreshProjectsWithNewTargetsList(event.getProject(), amendData.getTargetSpecs(), amendData.getPantsBspData());
    } catch (Throwable e) {
      logger.error(e);
    }
  }

  private void refreshProjectsWithNewTargetsList(
    @NotNull Project project,
    Collection<String> newTargets,
    PantsBspData basePath
  ) {
    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
      try {
        FastpassUtils.amendAll(basePath, new ArrayList<>(newTargets), project).get();
        ExternalProjectUtil.refresh(project, BSP.ProjectSystemId());
      } catch (Throwable e){
        logger.error(e);
      }
    }, "Amending", false, project);
  }
}
