// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.bsp.PantsBspData;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Completion;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
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
    @NotNull Function<VirtualFile, CompletableFuture<Collection<PantsTargetAddress>>> targetsListFetcher
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

  CompletableFuture<Collection<PantsTargetAddress>> validateAndGetDetails(String targetString) {
    Optional<PantsTargetAddress> pantsTarget  = PantsTargetAddress.tryParse(targetString);
    if(!pantsTarget.isPresent()) {
      return failedFuture(new InvalidTargetException(targetString, "Malformed address"));
    }

    if(resolvePathToFile(pantsTarget.get().getPath()) == null) {
      return failedFuture(new InvalidTargetException(targetString, "No such folder"));
    }

    if(pantsTarget.get().getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP ||
       pantsTarget.get().getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_FLAT ) {
      return mapToSingleTarget(pantsTarget.get());
    }else {
      Path path = pantsTarget.get().getPath();
      VirtualFile file = resolvePathToFile(path);
      CompletableFuture<Collection<PantsTargetAddress>> fut = myTargetsListFetcher.apply(file);
      return fut.thenApply(targets-> {
        if(targets.stream().noneMatch(target -> Objects.equals(target, pantsTarget.get()))) {
          throw new CompletionException(new InvalidTargetException(pantsTarget.toString(), "No such target"));
        } else {
          return Collections.singletonList(pantsTarget.get());
        }
      });
    }
  }

  @NotNull
  private CompletableFuture<Collection<PantsTargetAddress>> failedFuture(InvalidTargetException ex) {
    return CompletableFuture.supplyAsync(() ->
                                             {
                                               throw new CompletionException(ex);
                                             });
  }

  private void validateItems(Set<String> targetString) {
    preview.setLoading();
    statusLabel.setText("Validating");

    this.targetString = targetString;

    List<CompletableFuture<Collection<PantsTargetAddress>>> futures =
      targetString.stream().map(x -> validateAndGetDetails(x)).collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete(
      (value, error ) ->{
        SwingUtilities.invokeLater(() -> {
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
        });
      }
    );
  }

  private VirtualFile resolvePathToFile(Path path) {
    return myImportData.getPantsRoot().findFileByRelativePath(path.toString());
  }

  static class InvalidTargetException extends Throwable {
    private final String myTargetString;
    private final String myMessage;

    InvalidTargetException(String targetString, String message) {
      myTargetString = targetString;
      myMessage = message;
    }

    @Override
    public String getMessage() {
      return "[" + myTargetString + "]:" + myMessage;
    }
  }

  CompletableFuture<Collection<PantsTargetAddress>> mapToSingleTarget(PantsTargetAddress targetAddress) {
    switch (targetAddress.getKind()){
      case SINGLE_TARGET: {
        return CompletableFuture.completedFuture(Collections.singletonList(targetAddress));
      }
      case ALL_TARGETS_DEEP: {
        return myTargetsListFetcher.apply(resolvePathToFile(targetAddress.getPath()));
      }
      case ALL_TARGETS_FLAT: { return myTargetsListFetcher.apply(resolvePathToFile(targetAddress.getPath()))
          .thenApply(x -> x.stream()
            .filter(t -> t.getPath().equals(targetAddress.getPath()))
            .collect(Collectors.toSet()));
      }
    }
    return null; // todo no null
  }
}
