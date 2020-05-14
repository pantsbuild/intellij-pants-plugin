// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.bsp.FastpassTargetListCache;
import com.twitter.intellij.pants.bsp.FastpassUtils;
import com.twitter.intellij.pants.bsp.PantsBspData;
import com.twitter.intellij.pants.bsp.PantsTargetsRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.PantsBundle;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class FastpassManagerDialog extends DialogWrapper {
  public FastpassManagerDialog(
    @NotNull Project project,
    @NotNull CompletableFuture<Set<String>> importedTargets,
    @NotNull PantsTargetsRepository targetsListFetcher,
    @NotNull Collection<String> newTargets
  ) {
    super(project, false);
    setTitle(PantsBundle.message("pants.bsp.select.targets"));
    init();

    importedTargets.whenComplete(
      (targets, error) ->
        SwingUtilities.invokeLater(() -> {
          if (error == null) {
            showFastpassChooseTargetsPanel(targetsListFetcher, targets, newTargets);
          }
          else {
            logger.error(error);
            showCurrentTargetsFetchError();
          }
        }));
  }

  private void showFastpassChooseTargetsPanel(
    @NotNull PantsTargetsRepository targetsListFetcher,
    Set<String> targets,
    Collection<String> newTargets
  ) {
    mainPanel.removeAll();
    myChooseTargetsPanel = new FastpassEditTargetSpecsPanel(targets, targetsListFetcher, newTargets);
    mainPanel.add(myChooseTargetsPanel);
    setOKButtonText(CommonBundle.getOkButtonText());
    mainPanel.updateUI();
  }

  private void showCurrentTargetsFetchError() {
    mainPanel.removeAll();
    mainPanel.add(new JLabel(
      PantsBundle.message("pants.bsp.error.failed.to.fetch.targets"),
      PlatformIcons.ERROR_INTRODUCTION_ICON,
      SwingConstants.CENTER
    ));
    mainPanel.updateUI();
  }

  @NotNull
  static final Logger logger = Logger.getInstance(FastpassManagerDialog.class);

  FastpassEditTargetSpecsPanel myChooseTargetsPanel;

  @NotNull final JPanel mainPanel = new JPanel();

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    mainPanel.setLayout(new BorderLayout());
    mainPanel.setPreferredSize(JBUI.size(800, 600));
    mainPanel.add(new AsyncProcessIcon(""), BorderLayout.CENTER);
    return mainPanel;
  }

  public Optional<Collection<String>> selectedItems() {
    return Optional.ofNullable(myChooseTargetsPanel)
      .map(FastpassEditTargetSpecsPanel::selectedItems);
  }

  public static Optional<Set<String>> promptForTargetsToImport(
    Project project,
    CompletableFuture<Set<String>> importedTargets,
    PantsTargetsRepository fetchTargetsList,
    Collection<String> newTargets
  ) {
    try {
      FastpassManagerDialog dial =
        new FastpassManagerDialog(project, importedTargets, fetchTargetsList, newTargets);
      dial.show();
      return dial.isOK() ? dial.selectedItems().map(HashSet::new) : Optional.empty();
    }catch (Throwable e) {
      logger.error(e);
      return Optional.empty();
    }
  }

  public static Optional<Set<String>> promptForNewTargetSpecs(Project project,
                                                              PantsBspData firstProject,
                                                              CompletableFuture<Set<String>> oldTargets,
                                                              Collection<String> newTargets) {
    FastpassTargetListCache targetsListCache = new FastpassTargetListCache(Paths.get(firstProject.getPantsRoot().getPath()));
    PantsTargetsRepository getPreview = targets -> FastpassUtils.validateAndGetPreview(firstProject.getPantsRoot(), targets,
                                                                                       targetsListCache::getTargetsList
    );
    return FastpassManagerDialog
      .promptForTargetsToImport(project, oldTargets, getPreview, newTargets);
  }
}
