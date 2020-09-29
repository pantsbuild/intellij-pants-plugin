// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class JarMappings {
  private static final String SOURCES_JAR_SUFFIX = "-sources.jar";
  private static final Logger LOG = Logger.getInstance(JarMappings.class);

  public static JarMappings getInstance(Project project) {
    return ServiceManager.getService(project, JarMappings.class);
  }

  public JarMappings(Project project) {
    this.project = project;

    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        Optional<VirtualFile> libraries = librariesFile();
        events.forEach(event -> {
          if (event instanceof VFileContentChangeEvent &&
              event.getFile() != null &&
              libraries.isPresent() &&
              event.getFile().equals(libraries.get())) {
            librariesFileIsUpToDate = false;
          }
        });
      }
    });
  }

  private final Project project;

  private Map<String, String> libraryJarToLibrarySourceJar = new HashMap<>();
  private boolean librariesFileIsUpToDate = false;

  private synchronized void ensureUpToDate() {
    try {
      if (!librariesFileIsUpToDate) {
        Optional<VirtualFile> file = librariesFile();
        if(file.isPresent()) {
          String content = new String(file.get().contentsToByteArray());
          Type mapType = new TypeToken<Map<String, String>>() {}.getType();
          libraryJarToLibrarySourceJar = new Gson().fromJson(content, mapType);
          librariesFileIsUpToDate = true;
        }
      }
    }
    catch (Exception e) {
      librariesFileIsUpToDate = false;
      LOG.warn("Error while reading libraries.json", e);
    }
  }

  public Optional<String> findSourceJarForLibraryJar(VirtualFile path) {
    ensureUpToDate();
    return Optional.ofNullable(libraryJarToLibrarySourceJar.get(path.getPath()));
  }

  public Optional<String> findTargetForClassJar(VirtualFile jar) {
    if (!isProjectInternalDependency(jar)) {
      return Optional.empty();
    }
    String jarName = jar.getNameWithoutExtension();
    String targetAddress = targetAddressFromSanitizedFileName(jarName);
    return Optional.of(targetAddress);
  }

  public Optional<String> findTargetForJar(VirtualFile jar) {
    Optional<String> sourceJar = findTargetForSourceJar(jar);
    if(sourceJar.isPresent()){
      return sourceJar;
    } else {
      return findTargetForClassJar(jar);
    }
  }

  public Optional<String> findSourceJarForTarget(String target) {
    String name = target.replaceAll("[:/]", ".") + SOURCES_JAR_SUFFIX;
    Optional<Path> path = bloopJarsPath(project).map(p -> p.resolve(name));
    return path.map(Path::toString);
  }

  public Optional<String> findTargetForSourceJar(VirtualFile jar) {
    if (!isProjectInternalDependency(jar)) {
      return Optional.empty();
    }
    String name = jar.getName();
    if (name.endsWith(SOURCES_JAR_SUFFIX)) {
      String withoutSuffix = name.substring(0, name.length() - SOURCES_JAR_SUFFIX.length());
      return Optional.of(targetAddressFromSanitizedFileName(withoutSuffix));
    }
    else {
      return Optional.empty();
    }
  }

  private String targetAddressFromSanitizedFileName(String jarName) {
    String[] components = jarName.split("\\.");
    String[] targetPath = Arrays.copyOf(components, components.length - 1);
    String targetName = components[components.length - 1];
    return String.join("/", targetPath) + ":" + targetName;
  }

  public boolean isProjectInternalDependency(VirtualFile jar) {
    Path jarPath = Paths.get(jar.getPath());
    return isBloopJarPath(jarPath) || isSourcesJarPath(jarPath);
  }

  @NotNull
  private Boolean isSourcesJarPath(@NotNull Path jarPath) {
    return sourcesJarPath(project).map(jarPath::startsWith).orElse(false);
  }

  @NotNull
  private Boolean isBloopJarPath(@NotNull Path jarPath) {
    return bloopJarsPath(project).map(jarPath::startsWith).orElse(false);
  }

  @NotNull
  private static Optional<Path> bloopJarsPath(Project project) {
    return bspDir(project).map(path -> Paths.get(path, ".bloop", "bloop-jars"));
  }

  @NotNull
  private static Optional<Path> sourcesJarPath(Project project) {
    return bspDir(project).map(path -> Paths.get(path, ".bloop", "sources-jar"));
  }

  @NotNull
  private static Optional<String> bspDir(@NotNull Project project) {
    return PantsBspData.bspRoot(project).map(Path::toString);
  }

  private Optional<VirtualFile> librariesFile() {
    return bspDir(project)
      .map(bspImport -> {
        Path path = Paths.get(bspImport, ".pants", "libraries.json");
        return LocalFileSystem.getInstance().findFileByIoFile(path.toFile());
      });
  }

  public static Optional<VirtualFile> getParentJar(VirtualFile file) {
    return predecessors(file).stream().filter(x -> Objects.equals(x.getExtension(), "jar")).findFirst();
  }

  @NotNull
  public static List<VirtualFile> predecessors(@NotNull VirtualFile file) {
    List<VirtualFile> predecessors = Lists.newArrayList();
    VirtualFile parent = file;
    while(parent != null){
      predecessors.add(parent);
      parent = parent.getParent();
    }
    return predecessors;
  }
}
