// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.bsp.PantsBspData;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;


class FastpassChooseTargetsPanel extends JPanel {
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
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
    myFileSystemTree = createFileTree();
    JScrollPane fileSystemTreeScrollPane = ScrollPaneFactory.createScrollPane(myFileSystemTree.getTree());
    fileSystemTreeScrollPane.setPreferredSize(JBUI.size(400, 500));
    mainPanel.add(fileSystemTreeScrollPane);

    myTargetsListPanel = new FastpassAddressesViewPanel(
    );
    mainPanel.add(myTargetsListPanel);

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.add(mainPanel);
  }

  @NotNull
  Project myProject;

  @NotNull
  Function<VirtualFile, CompletableFuture<Collection<PantsTargetAddress>>> myTargetsListFetcher;

  @NotNull
  private PantsBspData myImportData;

  @NotNull
  Set<PantsTargetAddress> mySelectedTargets;

  JPanel mainPanel = null;

  FileSystemTreeImpl myFileSystemTree = null;

  FastpassAddressesViewPanel myTargetsListPanel = null;

  @NotNull
  public Collection<PantsTargetAddress> selectedItems() {
    return mySelectedTargets;
  }


  private FileSystemTreeImpl createFileTree() {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false,
                                                                 false, false,
                                                                 false, false
    );
    FileSystemTreeImpl fileSystemTree = new FileSystemTreeImpl(myProject, descriptor, new Tree(), null, null, null);
    fileSystemTree.select(myImportData.getPantsRoot(), null);
    fileSystemTree.expand(myImportData.getPantsRoot(), null);
    fileSystemTree.showHiddens(true);
    fileSystemTree.updateTree();
    fileSystemTree.getTree().getSelectionModel().addTreeSelectionListener(event -> handleTreeSelection(fileSystemTree));
    return fileSystemTree;
  }

  private void handleTreeSelection(FileSystemTreeImpl myFileSystemTree) {
    VirtualFile selectedFile = myFileSystemTree.getSelectedFile();
    if (selectedFile != null && belongsToImportedPantsProject(selectedFile, myImportData.getPantsRoot())
    ) {
      updateCheckboxList(selectedFile);
    } else {
      myTargetsListPanel.clear();
      this.updateUI();
    }
  }

  private boolean belongsToImportedPantsProject(
    VirtualFile selectedFile,
    VirtualFile root
  ) {
    return  Paths.get(selectedFile.getPath()).startsWith(Paths.get(root.getPath())) && !root.getPath().equals(selectedFile.getPath());
  }

  private void updateCheckboxList(VirtualFile selectedFile) {
    CompletableFuture<Collection<PantsTargetAddress>> targetsList = myTargetsListFetcher.apply(selectedFile);
    if (!targetsList.isDone()) {
      myTargetsListPanel.setLoading();
      mainPanel.updateUI();
    }
    targetsList.whenComplete((targetsInDir, error) -> {
      SwingUtilities.invokeLater(() -> {
        if (Objects.equals(myFileSystemTree.getSelectedFile(), selectedFile)) {
          if (error == null) {
            Path path = Paths.get(myImportData.getPantsRoot().getPath()).relativize(Paths.get(selectedFile.getPath()));
            myTargetsListPanel.setItems(
              targetsInDir,
              mySelectedTargets,
              path,
              items -> {
                mySelectedTargets.removeIf(x -> x.getPath().equals(path));
                mySelectedTargets.addAll(items);
              }
            );
            mainPanel.updateUI();
          } else {
            logger.error(error);
            myTargetsListPanel.fetchTargetsError();
            mainPanel.updateUI();
          }
        }
      });
    });
  }
}
