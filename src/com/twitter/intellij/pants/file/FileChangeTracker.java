// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.file;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

// FIXME: Change in pants.ini, `./pants clean-all` is not tracked currently.
public class FileChangeTracker {
  private static final Logger LOG = Logger.getInstance(FileChangeTracker.class);

  private static FileChangeTracker instance = new FileChangeTracker();

  // One to one relation between VirtualFileListener and Project,
  // so whenever a VirtualFileListener is triggered, we know which Project is affected.
  private static ConcurrentHashMap<VirtualFileListener, Project> listenToProjectMap = new ConcurrentHashMap<>();

  // Maps from Project to <isDirty, lastTargetsToCompile>
  private static ConcurrentHashMap<Project, Pair<Boolean, Optional<CompileSnapshot>>> projectStates = new ConcurrentHashMap<>();

  FileChangeTracker getInstance() {
    return instance;
  }

  private static void markDirty(@NotNull VirtualFile file, @NotNull VirtualFileListener listener) {
    Project project = listenToProjectMap.get(listener);

    boolean inProject = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(file) != null;
    LOG.debug(String.format("Changed: %s. In project: %s", file.getPath(), inProject));
    if (inProject) {
      markDirty(project);
    }
  }

  public static void markDirty(@NotNull Project project) {
    projectStates.put(project, Pair.create(true, Optional.empty()));
  }

  /**
   * Determine whether a project should be recompiled given targets to compile and PantsSettings
   * by comparing with the last one.
   * <p>
   * Side effect: if the answer is yes (true), it will also reset the project state.
   *
   * @param project:         project under question.
   * @param targetAddresses: target addresses for this compile.
   * @return true if anything in the project has changed or the current `CompileSnapshot` does not match with
   * the previous one.
   */
  public static boolean shouldRecompileThenReset(@NotNull Project project, @NotNull Set<String> targetAddresses) {
    PantsSettings settings = PantsSettings.getInstance(project);

    Pair<Boolean, Optional<CompileSnapshot>> pair = projectStates.get(project);
    CompileSnapshot snapshot = new CompileSnapshot(targetAddresses, settings);
    // there is no previous record.
    if (pair == null) {
      resetProjectState(project, snapshot);
      return true;
    }

    Optional<CompileSnapshot> previousSnapshot = pair.getSecond();
    if (
      // Recompile if project is in incremental mode, so there is no way to keep track of the all changes
      // in the transitive graph.
      settings.isEnableIncrementalImport()
      // Recompile if project is dirty or there is no previous record.
      || (pair.getFirst())
      // Recompile if there is no previous record.
      || !previousSnapshot.isPresent()
      // Recompile if current snapshot is different from previous one.
      // Then reset snapshot.
      || (!snapshot.equals(previousSnapshot.get()))
      // if manifest is not valid any more.
      || !isManifestJarValid(project)
      ) {
      resetProjectState(project, snapshot);
      return true;
    }

    return false;
  }

  /**
   * Check whether all the class path entries in the manifest are valid.
   *
   * @param project current project.
   * @return true iff the manifest jar is valid.
   */
  private static boolean isManifestJarValid(@NotNull Project project) {
    Optional<VirtualFile> manifestJar = PantsUtil.findProjectManifestJar(project);
    if (!manifestJar.isPresent()) {
      return false;
    }
    VirtualFile file = manifestJar.get();
    if (!new File(file.getPath()).exists()) {
      return false;
    }
    try {
      VirtualFile manifestInJar =
        VirtualFileManager.getInstance().refreshAndFindFileByUrl("jar://" + file.getPath() + "!/META-INF/MANIFEST.MF");
      if (manifestInJar == null) {
        return false;
      }
      Manifest manifest = new Manifest(manifestInJar.getInputStream());
      List<String> relPaths = PantsUtil.parseCmdParameters(Optional.of(manifest.getMainAttributes().getValue("Class-Path")));
      for (String path : relPaths) {
        // All rel paths in META-INF/MANIFEST.MF is relative to the jar directory
        if (!new File(file.getParent().getPath(), path).exists()) {
          return false;
        }
      }
      return true;
    }
    catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  private static void resetProjectState(@NotNull Project project, CompileSnapshot snapshot) {
    projectStates.put(project, Pair.create(false, Optional.of(snapshot)));
  }

  public static void registerProject(@NotNull Project project) {
    VirtualFileListener listener = getNewListener();
    LocalFileSystem.getInstance().addVirtualFileListener(listener);
    listenToProjectMap.put(listener, project);
  }

  public static void unregisterProject(@NotNull Project project) {
    projectStates.remove(project);

    // Remove the listener for the project.
    listenToProjectMap.entrySet().stream()
      .filter(s -> s.getValue() == project)
      .findFirst()
      .ifPresent(x -> {
        VirtualFileListener listener = x.getKey();
        listenToProjectMap.remove(listener);
        LocalFileSystem.getInstance().removeVirtualFileListener(listener);
      });
  }

  private static VirtualFileListener getNewListener() {
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
        FileChangeTracker.markDirty(event.getFile(), this);
      }

      @Override
      public void beforeFileMovement(@NotNull VirtualFileMoveEvent event) {
        FileChangeTracker.markDirty(event.getFile(), this);
      }
    };
  }

  /**
   * `CompileSnapshot` keeps track of `PantsSettings` and set of target addresses used to compile
   * at a given time.
   */
  private static class CompileSnapshot {
    Set<String> myTargetAddresses;
    PantsSettings myPantsSettings;

    private CompileSnapshot(Set<String> targetAddresses, PantsSettings pantsSettings) {
      myTargetAddresses = Collections.unmodifiableSet(targetAddresses);
      myPantsSettings = PantsSettings.copy(pantsSettings);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      CompileSnapshot other = (CompileSnapshot) obj;
      return Objects.equals(this.myPantsSettings, other.myPantsSettings)
             && Objects.equals(this.myTargetAddresses, other.myTargetAddresses);
    }
  }
}
