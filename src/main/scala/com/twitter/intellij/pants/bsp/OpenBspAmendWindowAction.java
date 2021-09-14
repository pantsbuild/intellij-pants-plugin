// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.bsp.ui.FastpassManagerDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.bsp.BspUtil;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class OpenBspAmendWindowAction extends AnAction {

  private final static Logger logger = Logger.getInstance(OpenBspAmendWindowAction.class);

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    Project project = e.getProject();
    if(project != null) {
      boolean isBsp = BspUtil.isBspProject(project);
      boolean amendInProgress = ServiceManager.getService(project, AmendService.class).isAmendProcessOngoing();
      e.getPresentation().setVisible(isBsp);
      e.getPresentation().setEnabled(!amendInProgress);
    } else {
      e.getPresentation().setVisible(false);
      e.getPresentation().setEnabled(false);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    try {
      Project project = event.getProject();
      bspAmendWithDialog(project, Collections.emptyList());
    } catch (Throwable e) {
      logger.error(e);
    }
  }

  public static void bspAmendWithDialog(Project project, Collection<String> targetsToAppend) {
    if (project != null) {
      Optional<PantsBspData> imports = PantsBspData.importsFor(project);
      if (imports.isPresent()) {
        startAmendProcedure(project, imports.get(), targetsToAppend);
      } else {
        Messages.showErrorDialog(
          PantsBundle.message("pants.bsp.error.failed.not.a.bsp.pants.project.message"),
          PantsBundle.message("pants.bsp.error.action.not.supported.title")
        );
      }
    } else {
      Messages.showErrorDialog(
        PantsBundle.message("pants.bsp.error.no.project.found"),
        PantsBundle.message("pants.bsp.error.action.not.supported.title")
      );
    }
  }

  private static void startAmendProcedure(Project project, PantsBspData firstProject, Collection<String> targetSpecs) {
    CompletableFuture<Set<String>> oldTargets = FastpassUtils.selectedTargets(firstProject);
    Optional<Set<String>> newTargets = FastpassManagerDialog.promptForNewTargetSpecs(project, firstProject, oldTargets, targetSpecs);
    amendAndRefreshIfNeeded(project, firstProject, oldTargets, newTargets);
  }

  private static void amendAndRefreshIfNeeded(
    @NotNull Project project,
    @NotNull PantsBspData basePath,
    @NotNull CompletableFuture<Set<String>> oldTargets,
    @NotNull Optional<Set<String>> newTargets
  ) {
    oldTargets.thenAccept(
      oldTargetsVal -> newTargets.ifPresent(newTargetsVal -> {
        if(newTargetsVal.isEmpty()) {
          SwingUtilities.invokeLater(() -> {
            Messages.showErrorDialog(PantsBundle.message("pants.bsp.msg.box.empty.list.content"),
                                     PantsBundle.message("pants.bsp.msg.box.amend.title"));
          });
        } else if (!newTargetsVal.equals(oldTargetsVal)) {
          try {
            DataContext c = dataId -> {
              if(AmendAction.amendDataKey.is(dataId)) {
                return new AmendData(basePath, newTargets.get());
              } else if(CommonDataKeys.PROJECT.is(dataId)){
                return project;
              } else {
                return null;
              }
            };

            AnAction a = ActionManager.getInstance().getAction("com.twitter.intellij.pants.bsp.AmendAction");
            AnActionEvent e = AnActionEvent.createFromAnAction(a, null, ActionPlaces.UNKNOWN, c);
            a.actionPerformed(e);
          }
          catch (Throwable e) {
            logger.error(e);
          }
        } else {
          SwingUtilities.invokeLater(() -> {
            Messages.showInfoMessage(PantsBundle.message("pants.bsp.msg.box.specs.unchanged.content"),
                                     PantsBundle.message("pants.bsp.msg.box.amend.title"));
          });
        }
      })
    );
  }
}
