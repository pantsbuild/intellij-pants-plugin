// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.bsp.PantsBspData;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class FastpassChooseTargetsPanel extends JPanel {
  private final JLabel statusLabel;
  private final TargetsPreview preview;
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
    mainPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    statusLabel = new JLabel();
    statusLabel.setText(" ");


    preview = new TargetsPreview();

    editor = new JTextArea(); // todo jscroll for editor
    editor.setPreferredSize(JBUI.size(200, 200));
    editor.setText(importedTargets.stream().map(PantsTargetAddress::toAddressString).collect(Collectors.joining("\n")));
    try {
      validateItems();
      preview.updatePreview(mySelectedTargets, this::mapToSingleTarget);
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
          preview.updatePreview(mySelectedTargets, x -> mapToSingleTarget(x));
        } catch (Throwable ex){
          logger.error(ex);
        }
      }
    });
    mainPanel.add(editor);
    mainPanel.add(preview);
    mainPanel.add(statusLabel);

    editor.setAlignmentX(JTextArea.LEFT_ALIGNMENT);
    preview.setAlignmentX(JTextArea.LEFT_ALIGNMENT);
    statusLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);


    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.setAlignmentX(Component.LEFT_ALIGNMENT);
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
  Function<VirtualFile, CompletableFuture<Collection<PantsTargetAddress>>> myTargetsListFetcher;

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

  private void validateItems() throws ExecutionException, InterruptedException {
    statusLabel.setText("Validating");

    List<Optional<PantsTargetAddress>> targets = selectedTargetStrings().stream().map(PantsTargetAddress::tryParse).collect(Collectors.toList());

    for(String line: selectedTargetStrings()) {
      Optional<PantsTargetAddress> pantsTarget  = PantsTargetAddress.tryParse(line);
      if(!pantsTarget.isPresent()) {
        statusLabel.setText("Malformed address: " + line);
        return;
      } else if(resolvePathToFile(pantsTarget.get().getPath()) == null) {
        statusLabel.setText("No such folder:" + line);
        return;
      } else if (pantsTarget.get().getKind() == PantsTargetAddress.AddressKind.SINGLE_TARGET) {
        Path path = pantsTarget.get().getPath();
        VirtualFile file = resolvePathToFile(path);
        CompletableFuture<Collection<PantsTargetAddress>> fut = myTargetsListFetcher.apply(file);
        if(fut.isDone()) {
          if(fut.get().stream().noneMatch(x -> Objects.equals(x, pantsTarget.get()))) {
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
      if(resolvePathToFile(pantsTarget.get().getPath()) != null) {
        VirtualFile file = resolvePathToFile(pantsTarget.get().getPath());
        myTargetsListFetcher.apply(file);
      }
    }
    mySelectedTargets = targets.stream().map(x -> x.get()).collect(Collectors.toSet());
    statusLabel.setText("Valid");
  }

  private VirtualFile resolvePathToFile(Path path) {
    return myImportData.getPantsRoot().findFileByRelativePath(path.toString());
  }

  CompletableFuture<Collection<PantsTargetAddress>> mapToSingleTarget(PantsTargetAddress targetAddress) {
    switch (targetAddress.getKind()){
      case SINGLE_TARGET: return CompletableFuture.completedFuture(Collections.singletonList(targetAddress));
      case ALL_TARGETS_DEEP: return myTargetsListFetcher.apply(resolvePathToFile(targetAddress.getPath()));
      case ALL_TARGETS_FLAT: return myTargetsListFetcher.apply(resolvePathToFile(targetAddress.getPath()))
        .thenApply(x -> x.stream()
          .filter(t -> t.getPath() == targetAddress.getPath())
          .collect(Collectors.toSet()));  // todo handle flat
    }
    return null; // todo no null
  }
}
