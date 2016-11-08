// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.file;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;


public class FileChangeTracker {
  private static boolean changed = false;
  private static FileChangeTracker instance = new FileChangeTracker();
  private static ConcurrentHashMap<Project, Boolean> projectStates = new ConcurrentHashMap<>();
  private static ConcurrentHashMap<VirtualFileListener, Project> listenToProjectMap = new ConcurrentHashMap<>(); // maps from project to changed

  FileChangeTracker getInstance() {
    return instance;
  }

  public static void setChanged(boolean hasChanged, @NotNull VirtualFile file, VirtualFileListener listener) {
    changed = hasChanged;
    Project project = listenToProjectMap.get(listener);
    boolean inProject = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(file) != null;
    System.out.println(String.format("Changed: %s. In project: %s", file.getPath(), inProject));
    if (inProject) {
      projectStates.put(project, true);
    }
  }

  public static boolean hasChanged(Project project) {
    if (projectStates.containsKey(project)) {
      return projectStates.get(project);
    }
    else {
      return true;
    }
  }

  public static void reset(Project project) {
    projectStates.put(project, false);
    System.out.println("Reset.");
  }

  public static void registerProject(Project project) {
    VirtualFileListener listener = getNewListener();
    LocalFileSystem.getInstance().addVirtualFileListener(listener);
    listenToProjectMap.put(listener, project);
  }

  public static VirtualFileListener getNewListener() {
    return new VirtualFileListener() {
      @Override
      public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {
        FileChangeTracker.setChanged(true, event.getFile(), this);
      }

      @Override
      public void contentsChanged(@NotNull VirtualFileEvent event) {
        FileChangeTracker.setChanged(true, event.getFile(), this);
      }

      @Override
      public void fileCreated(@NotNull VirtualFileEvent event) {
        FileChangeTracker.setChanged(true, event.getFile(), this);
      }

      @Override
      public void fileDeleted(@NotNull VirtualFileEvent event) {
        FileChangeTracker.setChanged(true, event.getFile(), this);
      }

      @Override
      public void fileMoved(@NotNull VirtualFileMoveEvent event) {
        FileChangeTracker.setChanged(true, event.getFile(), this);
      }

      @Override
      public void fileCopied(@NotNull VirtualFileCopyEvent event) {
        FileChangeTracker.setChanged(true, event.getFile(), this);
      }

      @Override
      public void beforePropertyChange(@NotNull VirtualFilePropertyEvent event) {
      }

      @Override
      public void beforeContentsChange(@NotNull VirtualFileEvent event) {
      }

      @Override
      public void beforeFileDeletion(@NotNull VirtualFileEvent event) {
      }

      @Override
      public void beforeFileMovement(@NotNull VirtualFileMoveEvent event) {
      }
    };
  }
}
