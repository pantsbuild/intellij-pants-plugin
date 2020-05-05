// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class SearchableFileSystemTree extends JPanel {

  FileSystemTreeImpl myFileSystemTree;
  JTextField myDirectoryTextField;

  private FileSystemTreeImpl createFileTree(Project project, VirtualFile root, Consumer<VirtualFile> onTreeSelection) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false,
                                                                 false, false,
                                                                 false, false
    );
    FileSystemTreeImpl fileSystemTree = new FileSystemTreeImpl(project, descriptor, new Tree(), null, null, null);
    fileSystemTree.select(root, null);
    fileSystemTree.expand(root, null);
    fileSystemTree.showHiddens(true);
    fileSystemTree.updateTree();
    fileSystemTree.getTree().getSelectionModel().addTreeSelectionListener(
      event -> {
        VirtualFile selectedFile = fileSystemTree.getSelectedFile();
        if(selectedFile != null) {
          Path relPath = Paths.get(root.getPath()).relativize(Paths.get(selectedFile.getPath()));
          myDirectoryTextField.setText(relPath.toString());
          fileSystemTree.expand(selectedFile, null);
          onTreeSelection.accept(selectedFile);
        }
      }
    );
    return fileSystemTree;
  }

  public SearchableFileSystemTree(Project project, VirtualFile root, Consumer<VirtualFile> onTreeSelection) {
    myFileSystemTree = createFileTree(project, root, onTreeSelection);
    myDirectoryTextField = new JTextField();
    myDirectoryTextField.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e) {

      }

      @Override
      public void keyPressed(KeyEvent e) {

      }

      @Override
      public void keyReleased(KeyEvent e) {
        VirtualFile newSelection = root.findFileByRelativePath(myDirectoryTextField.getText());
        if (newSelection != null) {
          myFileSystemTree.select(newSelection, null);
          myFileSystemTree.expand(newSelection, null);
        }
      }
    });

    this.add(myDirectoryTextField);

    JScrollPane fileSystemTreeScrollPane = ScrollPaneFactory.createScrollPane(myFileSystemTree.getTree());
    this.add(fileSystemTreeScrollPane);

    this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
  }
  public VirtualFile getSelectedFile() {
    return myFileSystemTree.getSelectedFile();
  }
}
