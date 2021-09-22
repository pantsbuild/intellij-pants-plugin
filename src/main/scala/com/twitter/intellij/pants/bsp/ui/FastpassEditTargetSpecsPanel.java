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
import javax.swing.JLabel;
import javax.swing.JPanel;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class FastpassEditTargetSpecsPanel extends JPanel {
  private final FastpassStatus statusLabel;
  private final TargetsPreview preview;
  final Logger logger = Logger.getInstance(FastpassEditTargetSpecsPanel.class);
  private final JLabel previewLabel;
  private Set<String> targetStrings;

  public FastpassEditTargetSpecsPanel(
    @NotNull Collection<String> alreadyImportedTargets,
    @NotNull PantsTargetsRepository targetsListFetcher,
    @NotNull Collection<String> targetsToAmend
  ) {
    myPantsTargetsRepository = targetsListFetcher;
    mainPanel = new JPanel();
    statusLabel = new FastpassStatus();
    preview = new TargetsPreview();

    previewLabel = new JLabel();
    previewLabel.setAlignmentX(LEFT_ALIGNMENT);

    editor = new JTextArea();
    editor.setText(alreadyImportedTargets.stream().sorted().collect(Collectors.joining("\n")) + "\n" +
                   targetsToAmend.stream().sorted().collect(Collectors.joining("\n")));
    onRulesListEdition(selectedTargetStrings());

    JLabel editorLabel = new JLabel(PantsBundle.message("pants.bsp.editor.title"));
    editorLabel.setAlignmentX(LEFT_ALIGNMENT);

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


    JPanel editorPanel = new JPanel();
    editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.PAGE_AXIS));
    editorPanel.add(editorLabel);
    JBScrollPane editorScroll = new JBScrollPane(editor);
    editorScroll.setAlignmentX(LEFT_ALIGNMENT);
    editorPanel.add(editorScroll);

    JPanel previewPanel = new JPanel();
    previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.PAGE_AXIS));
    previewPanel.add(previewLabel);
    JBScrollPane previewScroll = new JBScrollPane(preview);
    previewScroll.setAlignmentX(LEFT_ALIGNMENT);
    previewPanel.add(previewScroll);

    JPanel southPanel = new JPanel();
    southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.LINE_AXIS));

    JBSplitter northPanel = new JBSplitter(false);
    northPanel.setFirstComponent(editorPanel);
    northPanel.setSecondComponent(previewPanel);
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

  private void setPreviewTitle(int matches) {
    if(matches == 1 ) {
      previewLabel.setText(PantsBundle.message("pants.bsp.preview.title.singular", matches));
    } else {
      previewLabel.setText(PantsBundle.message("pants.bsp.preview.title.plural", matches));
    }
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
      setPreviewTitleLoading();
    }
    previewData.whenComplete(
      (value, error) -> SwingUtilities.invokeLater(() -> {
        if (this.targetStrings == targetStrings) {
          if (error == null) {
            Set<PantsTargetAddress> toPreview = value.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
            setPreviewTitle(toPreview.size());
            preview.updatePreview(toPreview);
            if(toPreview.size() > 0) {
              statusLabel.setOk();
            } else {
              statusLabel.setWarning(PantsBundle.message("pants.bsp.warn.no.targets"));
            }
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

  private void setPreviewTitleLoading() {
    previewLabel.setText(PantsBundle.message("pants.bsp.preview.title.loading"));
  }
}
