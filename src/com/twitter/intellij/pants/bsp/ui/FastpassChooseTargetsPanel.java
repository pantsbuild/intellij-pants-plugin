// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.bsp.PantsBspData;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;


class FastpassChooseTargetsPanel extends JPanel {
  private final JLabel statusLabel;
  Logger logger = Logger.getInstance(FastpassChooseTargetsPanel.class);


  public FastpassChooseTargetsPanel(
    @NotNull Project project,
    @NotNull PantsBspData importData,
    @NotNull Collection<PantsTargetAddress> importedTargets,
    @NotNull Function<VirtualFile, CompletableFuture<Collection<PantsTargetAddress>>> targetsListFetcher
  ) {
    myImportData = importData;
    mySelectedTargets = new HashSet<>(importedTargets);

    myProject = project;
    myTargetsListFetcher = targetsListFetcher;

    mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setAlignmentY(Component.LEFT_ALIGNMENT);

    statusLabel = new JLabel();
    statusLabel.setText(" ");

    editor = new JTextArea();
    editor.setPreferredSize(JBUI.size(200, 200));
    editor.setText(importedTargets.stream().map(PantsTargetAddress::toAddressString).collect(Collectors.joining("\n")));
    try {
      validateItems();
    } catch (Throwable e) {
      logger.error(e);
    }
    editor.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e) {

      }

      @Override
      public void keyPressed(KeyEvent e) {

      }

      @Override
      public void keyReleased(KeyEvent e) {
        try {
          mySelectedTargetStrings = selectedTargetStrings();
          validateItems();
        } catch (Throwable ex){
          // todo log
        }
      }
    });
    mainPanel.add(editor);

    myTargetsListPanel = new FastpassAddressesViewPanel();

    mainPanel.add(statusLabel);

    editor.setAlignmentX(JTextArea.LEFT_ALIGNMENT);
    statusLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);


    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.setAlignmentX(Component.LEFT_ALIGNMENT);
    this.add(mainPanel);
  }

  private Set<String> selectedTargetStrings() {
    String[] lines = editor.getText().split("\n");
    return Arrays.stream(lines).filter(x -> !x.equals("")).collect(Collectors.toSet());
  }

  @NotNull
  JTextArea editor;

  @NotNull
  Project myProject;

  @NotNull
  Function<VirtualFile, CompletableFuture<Collection<PantsTargetAddress>>> myTargetsListFetcher;

  @NotNull
  final private PantsBspData myImportData;

  @NotNull
  Set<PantsTargetAddress> mySelectedTargets;

  Set<String> mySelectedTargetStrings;

  JPanel mainPanel = null;

  FastpassAddressesViewPanel myTargetsListPanel = null;

  @NotNull
  public Collection<PantsTargetAddress> selectedItems() {
    return mySelectedTargets;
  }

  private void validateItems() throws ExecutionException, InterruptedException {
    statusLabel.setText("Validating");

    List<Optional<PantsTargetAddress>> targets = selectedTargetStrings().stream().map(PantsTargetAddress::tryParse).collect(Collectors.toList());

    for(String line: selectedTargetStrings()) {
      Optional<PantsTargetAddress> pantsTarget  = PantsTargetAddress.tryParse(line);
      if(!pantsTarget.isPresent()) {
        statusLabel.setText("Malformed address: " + line);
        return;
      } else if(myImportData.getPantsRoot().findFileByRelativePath(pantsTarget.get().getPath().toString()) == null) {
        statusLabel.setText("No such folder:" + line);
        return;
      } else if (pantsTarget.get().getKind() == PantsTargetAddress.AddressKind.SINGLE_TARGET) { VirtualFile file = myImportData.getPantsRoot().findFileByRelativePath(pantsTarget.get().getPath().toString());CompletableFuture<Collection<PantsTargetAddress>> fut = myTargetsListFetcher.apply(file);
        if(fut.isDone()) {
          if(!fut.get().stream().anyMatch(x -> Objects.equals(x, pantsTarget.get()))) {
            statusLabel.setText("No such target in path: " + pantsTarget.get().toAddressString());
            return;
          }
        } else {
          statusLabel.setText("Fetching targets: " + pantsTarget.get().toAddressString());
          fut.whenComplete((value, error) ->
                             SwingUtilities.invokeLater(() -> {
                               if (error == null) {
                                 try {
                                   validateItems();
                                 } catch (Throwable e ){
                                   logger.error(e);
                                 }
                               }
                             }));
          return;
        }
      }
      if(myImportData.getPantsRoot().findFileByRelativePath(pantsTarget.get().getPath().toString()) != null) {
        VirtualFile file = myImportData.getPantsRoot().findFileByRelativePath(pantsTarget.get().getPath().toString());
        myTargetsListFetcher.apply(file);
      }
    }
    mySelectedTargets = targets.stream().map(x -> x.get()).collect(Collectors.toSet());
    statusLabel.setText("Valid");
  }
}
