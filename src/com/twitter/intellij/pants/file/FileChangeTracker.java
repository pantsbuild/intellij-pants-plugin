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
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;


public class FileChangeTracker {
  private static FileChangeTracker instance = new FileChangeTracker();
  private static ConcurrentHashMap<Project, Boolean> projectStates = new ConcurrentHashMap<>();
  // Maps from project to changed
  private static ConcurrentHashMap<VirtualFileListener, Project> listenToProjectMap = new ConcurrentHashMap<>();

  FileChangeTracker getInstance() {
    return instance;
  }

  private static void markDirty(@NotNull VirtualFile file, @NotNull VirtualFileListener listener) {
    Project project = listenToProjectMap.get(listener);
    boolean inProject = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(file) != null;
    System.out.println(String.format("Changed: %s. In project: %s", file.getPath(), inProject));
    if (inProject) {
      markProjectDirty(project);
    }
  }

  public static void markProjectDirty(@NotNull Project project) {
    projectStates.put(project, true);
  }

  public static boolean hasChanged(@NotNull Project project) {
    if (projectStates.containsKey(project)) {
      return projectStates.get(project);
    }
    else {
      return true;
    }
  }

  public static void reset(@NotNull Project project) {
    projectStates.put(project, false);
    System.out.println("Reset.");
  }

  public static void registerProject(@NotNull Project project) {
    VirtualFileListener listener = getNewListener();
    LocalFileSystem.getInstance().addVirtualFileListener(listener);
    listenToProjectMap.put(listener, project);
  }

  public static void unregisterProject(@NotNull Project project) {
    if (projectStates.containsKey(project)) {
      projectStates.remove(project);
    }
    listenToProjectMap.entrySet().stream()
      .filter(s -> s.getValue() == project)
      .findFirst()
      .ifPresent(x -> {
        VirtualFileListener listener = x.getKey();
        listenToProjectMap.remove(listener);
        LocalFileSystem.getInstance().removeVirtualFileListener(listener);
      });
  }

  public static VirtualFileListener getNewListener() {
    return new VirtualFileListener() {
      @Override
      public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {
        FileChangeTracker.markDirty(event.getFile(), this);
      }

      @Override
      public void contentsChanged(@NotNull VirtualFileEvent event) {
        FileChangeTracker.markDirty(event.getFile(), this);
      }

      @Override
      public void fileCreated(@NotNull VirtualFileEvent event) {
        FileChangeTracker.markDirty(event.getFile(), this);
      }

      @Override
      public void fileDeleted(@NotNull VirtualFileEvent event) {
        FileChangeTracker.markDirty(event.getFile(), this);
      }

      @Override
      public void fileMoved(@NotNull VirtualFileMoveEvent event) {
        FileChangeTracker.markDirty(event.getFile(), this);
      }

      @Override
      public void fileCopied(@NotNull VirtualFileCopyEvent event) {
        FileChangeTracker.markDirty(event.getFile(), this);
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
