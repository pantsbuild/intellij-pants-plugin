// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.file;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListenerManager;
import com.twitter.intellij.pants.model.PantsOptions;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Manifest;

import static java.time.temporal.ChronoUnit.MILLIS;

// FIXME: Change in pants.ini, `./pants clean-all` is not tracked currently.
public class FileChangeTracker {
  private static final Logger LOG = Logger.getInstance(FileChangeTracker.class);
  public static final String HREF_REFRESH = "refresh";

  private static FileChangeTracker instance = new FileChangeTracker();

  // One to one relation between VirtualFileListener and Project,
  // so whenever a VirtualFileListener is triggered, we know which Project is affected.
  private static ConcurrentHashMap<VirtualFileListener, Project> listenToProjectMap = new ConcurrentHashMap<>();

  // Maps from Project to <myIsDirty, lastCompileSnapshot>
  private static ConcurrentHashMap<Project, ProjectState> projectStates = new ConcurrentHashMap<>();

  /**
   * Keep certain states about the current project.
   */
  private static class ProjectState {

    boolean myIsDirty;
    LocalTime myLastModifiedTime;
    Optional<CompileSnapshot> myLastCompileSnapshot;

    public ProjectState(
      boolean isDirty,
      LocalTime lastModified,
      Optional<CompileSnapshot> lastCompileSnapshot
    ) {
      myIsDirty = isDirty;
      myLastModifiedTime = lastModified;
      myLastCompileSnapshot = lastCompileSnapshot;
    }

    public boolean isDirty() {
      return myIsDirty;
    }

    public void setDirty(boolean dirty) {
      myIsDirty = dirty;
    }

    public LocalTime getLastModifiedTime() {
      return myLastModifiedTime;
    }

    public void setLastModifiedTime(LocalTime lastModifiedTime) {
      this.myLastModifiedTime = lastModifiedTime;
    }

    public Optional<CompileSnapshot> getLastCompileSnapshot() {
      return myLastCompileSnapshot;
    }

    public void setLastCompileSnapshot(Optional<CompileSnapshot> lastCompileSnapshot) {
      this.myLastCompileSnapshot = lastCompileSnapshot;
    }
  }

  public FileChangeTracker getInstance() {
    return instance;
  }

  private static void markDirty(@NotNull VirtualFile file, final PantsOptions pantsOptions, @NotNull VirtualFileListener listener) {
    Project project = listenToProjectMap.get(listener);

    ChangeType changeType = detectChangeType(project, pantsOptions, file);
    LOG.debug(String.format("Changed: %s. In project: %s", file.getPath(), changeType));
    if (changeType == ChangeType.UNRELATED) {
      return;
    }

    markDirty(project);

    if (changeType == ChangeType.BUILD) {
      ProjectRefreshListener.notify(project);
    }
  }

  enum ChangeType {
    UNRELATED,
    BUILD,
    OTHER;
  }

  private static ChangeType detectChangeType(Project project,final PantsOptions pantsOptions, @NotNull VirtualFile file) {
    ProjectRootManager rootManager = ProjectRootManager.getInstance(project);

    if (rootManager.getFileIndex().getContentRootForFile(file) != null) {
      boolean isBuildFile = PantsUtil.isBUILDFileName(file.getName());
      boolean isUnderPantsRepo = PantsUtil.isFileUnderPantsRepo(file);

      Path filePath = Paths.get(file.getPath());
      boolean shouldBeIgnored =
          pantsOptions.get(PantsConstants.PANTS_OPTION_PANTS_WORKDIR)
          .map(workdir -> filePath.startsWith(Paths.get(workdir).toAbsolutePath()))
          .orElse(filePath.toUri().toString().contains("/.pants.d/"));

      if (isUnderPantsRepo && isBuildFile && !shouldBeIgnored) {
        return ChangeType.BUILD;
      }

      return ChangeType.OTHER;
    }

    return ChangeType.UNRELATED;
  }

  public static void markDirty(@NotNull Project project) {
    final boolean isDirty = true;
    projectStates.put(project, new ProjectState(isDirty, LocalTime.now(), Optional.empty()));
  }

  public static void addManifestJarIntoSnapshot(@NotNull Project project) {
    Optional<CompileSnapshot> snapshot = projectStates.get(project).getLastCompileSnapshot();
    if (!snapshot.isPresent()) {
      return;
    }
    Optional<VirtualFile> manifestJar = PantsUtil.findProjectManifestJar(project);
    snapshot.get().setManifestJarHash(fileHash(manifestJar));
  }


  public static Optional<String> fileHash(Optional<VirtualFile> vf) {
    if (!vf.isPresent()) {
      return Optional.empty();
    }
    HashFunction hf = Hashing.md5();
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(vf.get().getPath()));
      HashCode hash = hf.newHasher().putBytes(bytes).hash();
      return Optional.of(hash.toString());
    }
    catch (IOException e) {
      e.printStackTrace();
      return Optional.empty();
    }
  }

  /**
   * Determine whether a project should be recompiled given targets to compile and PantsSettings
   * by comparing with the last one.
   * <p>
   * It assumes the compilation is going to work, if not, `markDirty` should be called explicitly upon failure.
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

    ProjectState lastRecordedState = projectStates.get(project);


    CompileSnapshot snapshot = new CompileSnapshot(targetAddresses, settings, PantsUtil.findProjectManifestJar(project));

    // there is no previous record.
    if (lastRecordedState == null) {
      resetProjectState(project, snapshot);
      return true;
    }
    if (lastRecordedState.isDirty()) {
      long betweenMilliSec = MILLIS.between(lastRecordedState.getLastModifiedTime(), LocalTime.now());
      PantsExternalMetricsListenerManager.getInstance().logDurationBeforePantsCompile(betweenMilliSec);
    }
    Optional<CompileSnapshot> previousSnapshot = lastRecordedState.getLastCompileSnapshot();
    if (
      // Recompile if project is in incremental mode, because there is no way to keep track of the all changes
      // in the transitive graph.
      settings.isEnableIncrementalImport()
      // Recompile if project is dirty or there is no previous record.
      || (lastRecordedState.isDirty())
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
      List<String> relPaths = PantsUtil.parseCmdParameters(manifest.getMainAttributes().getValue("Class-Path"));
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

  /**
   * Reset project to be clean.
   */
  private static void resetProjectState(@NotNull Project project, CompileSnapshot snapshot) {
    boolean isDirty = false;
    projectStates.put(project, new ProjectState(isDirty, LocalTime.now(), Optional.of(snapshot)));
  }

  public static void registerProject(@NotNull Project project, final PantsOptions pantsOptions) {
    VirtualFileListener listener = getNewListener(pantsOptions);
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

  private static VirtualFileListener getNewListener(final PantsOptions pantsOptions) {
    return new VirtualFileListener() {

      private void markDirty(VirtualFile file) {
        FileChangeTracker.markDirty(file, pantsOptions, this);
      }

      @Override
      public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {
        markDirty(event.getFile());
      }

      @Override
      public void contentsChanged(@NotNull VirtualFileEvent event) {
        markDirty(event.getFile());
      }

      @Override
      public void fileCreated(@NotNull VirtualFileEvent event) {
        markDirty(event.getFile());
      }

      @Override
      public void fileDeleted(@NotNull VirtualFileEvent event) {
        markDirty(event.getFile());
      }

      @Override
      public void fileMoved(@NotNull VirtualFileMoveEvent event) {
        markDirty(event.getFile());
      }

      @Override
      public void fileCopied(@NotNull VirtualFileCopyEvent event) {
        markDirty(event.getFile());
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

  /**
   * `CompileSnapshot` defines a moment with `PantsSettings`, set of target addresses used to compile,
   * and compiled manifest.jar.
   */
  private static class CompileSnapshot {
    Set<String> myTargetAddresses;
    Optional<String> myManifestJarHash;
    PantsSettings myPantsSettings;

    private void setManifestJarHash(Optional<String> manifestJarHash) {
      myManifestJarHash = manifestJarHash;
    }

    private CompileSnapshot(Set<String> targetAddresses, PantsSettings pantsSettings, Optional<VirtualFile> manifestJar) {
      myTargetAddresses = Collections.unmodifiableSet(targetAddresses);
      myPantsSettings = PantsSettings.copy(pantsSettings);
      myManifestJarHash = fileHash(manifestJar);
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
             && Objects.equals(this.myManifestJarHash, other.myManifestJarHash)
             && Objects.equals(this.myTargetAddresses, other.myTargetAddresses);
    }
  }
}
