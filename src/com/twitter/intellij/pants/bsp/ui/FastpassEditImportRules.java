// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.bsp.InvalidTargetException;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import com.twitter.intellij.pants.bsp.PantsTargetsRepository;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class FastpassEditImportRules extends JPanel {
  private final FastpassStatus statusLabel;
  private final TargetsPreview preview;
  final Logger logger = Logger.getInstance(FastpassEditImportRules.class);
  private Set<String> targetStrings;


  public FastpassEditImportRules(
    @NotNull Collection<String> importedTargets,
    @NotNull PantsTargetsRepository targetsListFetcher
  ) {
    myPantsTargetsRepository = targetsListFetcher;
    mainPanel = new JPanel();
    statusLabel = new FastpassStatus();
    preview = new TargetsPreview();
    editor = new JTextArea();
    editor.setText(importedTargets.stream().sorted().collect(Collectors.joining("\n")));
    onRulesListEdition(selectedTargetStrings());

    editor.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e) {}

      @Override
      public void keyPressed(KeyEvent e) {}

      @Override
      public void keyReleased(KeyEvent e) {
        onRulesListEdition(selectedTargetStrings());
      }
    });

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    JPanel southPanel = new JPanel();
    southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.LINE_AXIS));

    JBSplitter northPanel = new JBSplitter(false);
    northPanel.setFirstComponent(new JBScrollPane(editor));
    northPanel.setSecondComponent(new JBScrollPane(preview));
    northPanel.setProportion(0.35f);
    southPanel.add(statusLabel);

    southPanel.setAlignmentX(LEFT_ALIGNMENT);
    mainPanel.setAlignmentX(LEFT_ALIGNMENT);
    statusLabel.setAlignmentX(LEFT_ALIGNMENT);
    northPanel.setAlignmentX(LEFT_ALIGNMENT);

    mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.PAGE_AXIS));
    mainPanel.add(northPanel);
    mainPanel.add(southPanel);


    this.add(mainPanel);
  }

  private Set<String> selectedTargetStrings() {
    Stream<String> lines = Arrays.stream(editor.getText().split("\n"));
    return lines.filter(x -> !x.equals("")).collect(Collectors.toSet());
  }

  @NotNull final
  JTextArea editor;

  final JPanel mainPanel;

  @NotNull
  public Set<String> selectedItems() {
    return targetStrings;
  }


  final PantsTargetsRepository myPantsTargetsRepository;

  private void onRulesListEdition(Set<String> targetStrings) {
    this.targetStrings = targetStrings;
    CompletableFuture<Map<PantsTargetAddress, Collection<PantsTargetAddress>>>
      previewData = myPantsTargetsRepository.getPreview(targetStrings);
    if(!previewData.isDone()) {
      preview.setLoading();
      statusLabel.setLoading();
    }
    previewData.whenComplete(
      (value, error) -> SwingUtilities.invokeLater(() -> {
        if (this.targetStrings == targetStrings) {
          if (error == null) {
            Set<PantsTargetAddress> toPreview = value.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            preview.updatePreview(toPreview);
            statusLabel.setOk();
          }
          else {
            preview.setError();
            if (error instanceof CompletionException && error.getCause() instanceof InvalidTargetException) {
              statusLabel.setError(error.getCause().getMessage());
            }
            else {
              statusLabel.setError(PantsBundle.message("pants.bsp.unknown.error"));
              logger.error(error);
            }
          }
        }
      })
    );
  }
}
