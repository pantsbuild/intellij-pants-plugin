// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final public class FastpassUtils {

  @NotNull
  public static Stream<VirtualFile> pantsRoots(Module module) {
    return Stream.of(ModuleRootManager.getInstance(module).getSourceRoots()).flatMap (
      sourceRoot -> toStream(PantsUtil.findPantsExecutable(sourceRoot.getPath()).map(VirtualFile::getParent))
    );
  }

  public static CompletableFuture<Void> amendAll(@NotNull PantsBspData importData, Collection<String> newTargets) throws IOException {
    List<String> amendPart = Arrays.asList(
      "amend", importData.getBspPath().getFileName().toString(),
      "--new-targets", String.join(",", newTargets)
    );
    String[] command = makeFastpassCommand(amendPart);
    Process process = fastpassProcess(command, importData.getBspPath().getParent(), Paths.get(importData.getPantsRoot().getPath()));
    return onExit(process).thenAccept(__ -> {});
  }

  // instead of of JDK9's CompletableFuture::onExit
  @NotNull
  private static CompletableFuture<Process> onExit(@NotNull Process process) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        process.waitFor();
        if (process.exitValue() != 0) {
          throw new IOException(toString(process.getErrorStream()));
        }
        return process;
      } catch (Throwable e) {
        throw new CompletionException(e);
      }
    });
  }

  public static CompletableFuture<Set<PantsTargetAddress>> selectedTargets(PantsBspData basePath) throws IOException {
        String[] fastpassCommand = makeFastpassCommand(Arrays.asList("info", basePath.getBspPath().getFileName().toString()));
        Process process = fastpassProcess(fastpassCommand, basePath.getBspPath().getParent(), Paths.get(basePath.getPantsRoot().getPath()));
        return onExit(process).thenApply(finishedProcess -> {
          try {
            String stdout = toString(finishedProcess.getInputStream());
            String[] list = stdout.equals("") ? new String[]{} : stdout.split("\n");
            return Stream.of(list).map(PantsTargetAddress::fromString).collect(Collectors.toSet());
          } catch (Throwable e) {
            throw new CompletionException(e);
          }
        });
  }


  private static List<String> coursierPart = Arrays.asList(
    "coursier", "launch", "org.scalameta:metals_2.12:0.8.4+114-d75e2293-SNAPSHOT", "-r", "sonatype:snapshots",
    "--main", "scala.meta.internal.pantsbuild.BloopPants", "--"
  );

  @NotNull
  private static String[] makeFastpassCommand(@NotNull  Collection<String> amendPart) {
    return Stream.concat(coursierPart.stream(), amendPart.stream()).toArray(String[]::new);
  }

  @NotNull
  private static Process fastpassProcess(String[] command, Path fastpassHome, Path pantsWorkspace) throws IOException {
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.environment().put("FASTPASS_HOME", fastpassHome.toString());
    builder.directory(pantsWorkspace.toFile());
    return builder.start();
  }

  @NotNull
  private static <T>  Stream<T> toStream(@NotNull Optional<T> pantsExecutable) {
    if(pantsExecutable.isPresent()) {
      return Stream.of(pantsExecutable.get());
    } else {
      return Stream.empty();
    }
  }

  @NotNull
  private static String toString(InputStream process) throws IOException {
    return IOUtils.toString(process, StandardCharsets.UTF_8);
  }

  public static CompletableFuture<Collection<PantsTargetAddress>> availableTargetsIn(VirtualFile file) {
    return CompletableFuture.supplyAsync(
      () -> {
        final String buildFileName = "BUILD";
        if (file.isDirectory() && file.findChild(buildFileName) != null) {
          return PantsUtil.listAllTargets(Paths.get(file.getPath(), buildFileName).toString())
            .stream()
            .map(PantsTargetAddress::fromString)
            .collect(Collectors.toList());
        } else if(file.getName().equals(buildFileName)) {
          return PantsUtil.listAllTargets(file.getPath()).stream().map(PantsTargetAddress::fromString).collect(Collectors.toList());
        } else {
          return Collections.emptyList();
        }
      });
  }
}
