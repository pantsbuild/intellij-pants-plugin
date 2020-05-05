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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;


class FastpassChooseTargetsPanel extends JPanel {
  private final JLabel statusLabel;
  Logger logger = Logger.getInstance(FastpassChooseTargetsPanel.class);

  Path myPreviewCurrentPath  = null;

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

    preview = new JTextArea();
    preview.setPreferredSize(JBUI.size(200,200));
    preview.setText(importedTargets.stream().map(PantsTargetAddress::toAddressString).collect(Collectors.joining("\n")));
    validateItems();
    preview.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e) {

      }

      @Override
      public void keyPressed(KeyEvent e) {

      }

      @Override
      public void keyReleased(KeyEvent e) {
        try {

          int currentLine = preview.getLineOfOffset(preview.getCaretPosition());
          mySelectedTargetStrings = selectedTargetStrings();
          validateItems();
        } catch (Throwable ex){
          // todo log
        }
      }
    });
    mainPanel.add(preview);

    myTargetsListPanel = new FastpassAddressesViewPanel();

    mainPanel.add(statusLabel);

    preview.setAlignmentX(JTextArea.LEFT_ALIGNMENT);
    statusLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);


    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.setAlignmentX(Component.LEFT_ALIGNMENT);
    this.add(mainPanel);
  }

  private Set<String> selectedTargetStrings() {
    String[] lines = preview.getText().split("\n");
    return Arrays.stream(lines).filter(x -> !x.equals("")).collect(Collectors.toSet());
  }

  @NotNull
  JTextArea preview;

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


  private boolean belongsToImportedPantsProject(
    VirtualFile selectedFile,
    VirtualFile root
  ) {
    return  Paths.get(selectedFile.getPath()).startsWith(Paths.get(root.getPath())) && !root.getPath().equals(selectedFile.getPath());
  }

  private void validateItems() {
    statusLabel.setText("Validating");

    List<Optional<PantsTargetAddress>> targets = selectedTargetStrings().stream().map(PantsTargetAddress::tryParse).collect(Collectors.toList());

    for(String line: selectedTargetStrings()) {
      Optional<PantsTargetAddress> pantsTarget  = PantsTargetAddress.tryParse(line);
      if(!pantsTarget.isPresent()) {
        statusLabel.setText("Invalid address: " + line);
        return;
      } else if(myImportData.getPantsRoot().findFileByRelativePath(pantsTarget.get().getPath().toString()) == null){
        statusLabel.setText("No such folder:" + line);
        return;
      } else if (pantsTarget.get().getKind() == PantsTargetAddress.AddressKind.SINGLE_TARGET) {
        statusLabel.setText("Single target addresses not supported");
        return;
      }
    }
    mySelectedTargets = targets.stream().map(x -> x.get()).collect(Collectors.toSet());
    statusLabel.setText("Valid");
  }

  private void updateCheckboxList(Path path) {
    VirtualFile selectedFile = myImportData.getPantsRoot().findFileByRelativePath(path.toString());
    CompletableFuture<Collection<PantsTargetAddress>> targetsList = myTargetsListFetcher.apply(selectedFile);
    if (!targetsList.isDone()) {
      myTargetsListPanel.setLoading();
      mainPanel.updateUI();
    }
    targetsList.whenComplete((targetsInDir, error) -> {
      SwingUtilities.invokeLater(() -> {
        if (Objects.equals(myPreviewCurrentPath, path)) {
          if (error == null) {
//            Path path = Paths.get(myImportData.getPantsRoot().getPath()).relativize(Paths.get(selectedFile.getPath()));
            myTargetsListPanel.setItems(
              targetsInDir,
              mySelectedTargets,
              path,
              items -> {
 ;
              }
            );
            mainPanel.updateUI();
          } else {
            logger.error(error);
          }
        }
      });
    });
  }

}
