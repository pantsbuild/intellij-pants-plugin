// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.bsp.FastpassUtils;
import com.twitter.intellij.pants.bsp.PantsBspData;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class FastpassChooseTargetsPanel extends JPanel {
  private final JLabel statusLabel;
  private final TargetsPreview preview;
  Logger logger = Logger.getInstance(FastpassChooseTargetsPanel.class);
  private Set<String> targetString;


  public FastpassChooseTargetsPanel(
    @NotNull Project project,
    @NotNull PantsBspData importData,
    @NotNull Collection<PantsTargetAddress> importedTargets,
    @NotNull Function<Path, CompletableFuture<Collection<PantsTargetAddress>>> targetsListFetcher
  ) {
    myImportData = importData;
    mySelectedTargets = new HashSet<>(importedTargets);

    myProject = project;
    myTargetsListFetcher = targetsListFetcher;

    mainPanel = new JPanel();

    statusLabel = new JLabel();
    statusLabel.setText(" ");


    preview = new TargetsPreview();

    editor = new JTextArea();
    editor.setAlignmentX(Component.LEFT_ALIGNMENT);
    editor.setText(importedTargets.stream().map(PantsTargetAddress::toAddressString).collect(Collectors.joining("\n")));
    try {
      validateItems(selectedTargetStrings());
    } catch (Throwable e) {
      logger.error(e);
    }
    editor.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e) {}

      @Override
      public void keyPressed(KeyEvent e) {}

      @Override
      public void keyReleased(KeyEvent e) {
        try {
          mySelectedTargetStrings = selectedTargetStrings();
          validateItems(selectedTargetStrings());
        } catch (Throwable ex){
          logger.error(ex);
        }
      }
    });

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    JPanel northPanel = new JPanel();
    northPanel.setLayout(new GridLayout(0, 2));

    JPanel southPanel = new JPanel();
    southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.LINE_AXIS));

    JPanel editorPanel = new JPanel();
    editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.PAGE_AXIS));
    editorPanel.add(editor);
    northPanel.add(new JScrollPane(editorPanel));
    northPanel.add(new JScrollPane(preview));
    southPanel.add(statusLabel);

    southPanel.setAlignmentX(LEFT_ALIGNMENT);
    mainPanel.setAlignmentX(LEFT_ALIGNMENT);
    statusLabel.setAlignmentX(LEFT_ALIGNMENT);
    northPanel.setAlignmentX(LEFT_ALIGNMENT);
    southPanel.setAlignmentX(LEFT_ALIGNMENT);

    mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.PAGE_AXIS));
    mainPanel.add(northPanel);
    mainPanel.add(southPanel);


    this.add(mainPanel);
  }

  private Set<String> selectedTargetStrings() {
    Stream<String> lines = Arrays.stream(editor.getText().split("\n"));
    return lines.filter(x -> !x.equals("")).collect(Collectors.toSet());
  }

  @NotNull
  JTextArea editor;

  @NotNull
  Project myProject;

  @NotNull
  Function<Path, CompletableFuture<Collection<PantsTargetAddress>>> myTargetsListFetcher;

  @NotNull
  final private PantsBspData myImportData;

  @NotNull
  Set<PantsTargetAddress> mySelectedTargets;

  Set<String> mySelectedTargetStrings;

  JPanel mainPanel = null;


  @NotNull
  public Collection<PantsTargetAddress> selectedItems() {
    return mySelectedTargets;
  }


  private void validateItems(Set<String> targetString) {
    preview.setLoading();
    statusLabel.setText("Validating");

    this.targetString = targetString;

    List<CompletableFuture<Collection<PantsTargetAddress>>> futures =
      targetString.stream()
        .map(x -> FastpassUtils.validateAndGetDetails(myImportData.getPantsRoot(), x, myTargetsListFetcher)).collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete(
      (value, error ) -> SwingUtilities.invokeLater(() -> {
        if(this.targetString == targetString){
          if (error == null) {
            Set<PantsTargetAddress> toPreview = futures.stream().flatMap(x -> x.join().stream()).collect(Collectors.toSet());
            preview.updatePreview(toPreview);
            statusLabel.setText("Valid");
          }
          else {
            preview.setError();
            if (error instanceof CompletionException) {
              statusLabel.setText(error.getCause().getMessage());
            }
            else {
              statusLabel.setText("Invalid");
              logger.error(error);
            }
          }
        }
      })
    );
  }
}
