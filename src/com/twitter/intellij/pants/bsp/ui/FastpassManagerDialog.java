// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.bsp.PantsBspData;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class FastpassManagerDialog extends DialogWrapper {
  public FastpassManagerDialog(
    @NotNull Project project,
    @NotNull PantsBspData importData,
    @NotNull CompletableFuture<Set<PantsTargetAddress>> importedTargets,
    @NotNull Function<VirtualFile, CompletableFuture<Collection<PantsTargetAddress>>> targetsListFetcher
  ) {
    super(project, false);
    setTitle(PantsBundle.message("pants.bsp.select.targets"));
    init();

    importedTargets.whenComplete(
      (targets, error) ->
        SwingUtilities.invokeLater(() -> {
          if (error == null) {
            showFastpassChooseTargetsPanel(project, importData, targetsListFetcher, targets);
          }
          else {
            logger.error(error);
            showCurrentTargetsFetchError();
          }
        }));
  }

  private void showFastpassChooseTargetsPanel(
    @NotNull Project project,
    @NotNull PantsBspData importData,
    @NotNull Function<VirtualFile, CompletableFuture<Collection<PantsTargetAddress>>> targetsListFetcher,
    Set<PantsTargetAddress> targets
  ) {
    mainPanel.removeAll();
    myChooseTargetsPanel = new FastpassChooseTargetsPanel(project, importData, targets, targetsListFetcher);
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
  static Logger logger = Logger.getInstance(FastpassManagerDialog.class);

  FastpassChooseTargetsPanel myChooseTargetsPanel;

  @NotNull JPanel mainPanel = new JPanel();

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    mainPanel.setLayout(new BorderLayout());
    mainPanel.setPreferredSize(JBUI.size(800, 600));
    mainPanel.add(new AsyncProcessIcon(""), BorderLayout.CENTER);
    return mainPanel;
  }

  public Optional<Collection<PantsTargetAddress>> selectedItems() {
    return Optional.ofNullable(myChooseTargetsPanel)
      .map(FastpassChooseTargetsPanel::selectedItems);
  }

  public static Optional<Set<PantsTargetAddress>> promptForTargetsToImport(
    Project project,
    PantsBspData importData,
    CompletableFuture<Set<PantsTargetAddress>> importedTargets,
    Function<VirtualFile, CompletableFuture<Collection<PantsTargetAddress>>> fetchTargetsList
  ) {
    try {
      FastpassManagerDialog dial =
        new FastpassManagerDialog(project, importData, importedTargets, fetchTargetsList);
      dial.show();
      return dial.isOK() ? dial.selectedItems().map(HashSet::new) : Optional.empty();
    }catch (Throwable e) {
      logger.error(e);
      return Optional.empty();
    }
  }
}
