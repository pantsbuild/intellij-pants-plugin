// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.bsp.ui.FastpassManagerDialog;
import com.twitter.intellij.pants.util.ExternalProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.bsp.BSP;
import org.jetbrains.bsp.BspUtil;

import javax.swing.SwingUtilities;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FastpassBspAmendAction extends AnAction {

  private final Logger logger = Logger.getInstance(FastpassBspAmendAction.class);

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    boolean isBsp = e.getProject() != null && BspUtil.isBspProject(e.getProject());
    e.getPresentation().setEnabledAndVisible(isBsp);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    try {
      Project project = event.getProject();
      if (project != null) {
        Set<PantsBspData> linkedProjects = PantsBspData.importsFor(project);
        if (linkedProjects.size() > 1) {
          Messages.showErrorDialog(
            PantsBundle.message("pants.bsp.error.failed.more.than.one.bsp.project.not.supported.message"),
            PantsBundle.message("pants.bsp.error.action.not.supported.title")
          );
        }
        else if (linkedProjects.size() < 1) {
          Messages.showErrorDialog(
            PantsBundle.message("pants.bsp.error.failed.not.a.bsp.pants.project.message"),
            PantsBundle.message("pants.bsp.error.action.not.supported.title")
          );
        }
        else {
          PantsBspData importData = linkedProjects.stream().findFirst().get();
          startAmendProcedure(project, importData);
        }
      }
      else {
        Messages.showErrorDialog(
          PantsBundle.message("pants.bsp.error.no.project.found"),
          PantsBundle.message("pants.bsp.error.action.not.supported.title")
        );
      }
    } catch (Throwable e) {
      logger.error(e);
    }
  }

  private void startAmendProcedure(Project project, PantsBspData firstProject) {
    CompletableFuture<Set<String>> oldTargets = FastpassUtils.selectedTargets(firstProject);

    FastpassTargetListCache targetsListCache = new FastpassTargetListCache(Paths.get(firstProject.getPantsRoot().getPath()));
    PantsTargetsRepository getPreview = targets -> FastpassUtils.validateAndGetPreview(firstProject.getPantsRoot(), targets,
                                                                                       targetsListCache::getTargetsList
    );
    Optional<Set<String>> newTargets = FastpassManagerDialog
      .promptForTargetsToImport(project, oldTargets, getPreview);
    amendAndRefreshIfNeeded(project, firstProject, oldTargets, newTargets);
  }

  private void amendAndRefreshIfNeeded(
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
            refreshProjectsWithNewTargetsList(project, newTargets.get(), basePath);
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
    },"Amending", false, project );
  }
}
