// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.google.gson.Gson;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final public class FastpassUtils {

  @NotNull
  public static boolean fastpassBinaryExists(@NotNull VirtualFile file) {
    Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(file);
    return buildRoot.map(b -> Paths.get(b.getPath())
      .resolve("fastpass/bin/fastpass").toFile().exists())
      .orElse(false);
  }

  @NotNull
  public static Stream<VirtualFile> pantsRoots(Module module) {
    return Stream.of(ModuleRootManager.getInstance(module).getSourceRoots()).flatMap (
      sourceRoot -> toStream(PantsUtil.findPantsExecutable(sourceRoot.getPath()).map(VirtualFile::getParent))
    );
  }

  public static CompletableFuture<Set<String>> selectedTargets(PantsBspData basePath) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        VirtualFile bloopJsonFile =
          VirtualFileManager.getInstance().findFileByNioPath(basePath.getBspPath().resolve(Paths.get(".bsp", "bloop.json")));
        byte[] bytes = bloopJsonFile.contentsToByteArray(true);
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
        Gson gson = new Gson();
        BloopConfig res = gson.fromJson(reader, BloopConfig.class);
        return new HashSet<>(res.getPantsTargets());
      }
      catch (Throwable e) {
        throw new CompletionException(e);
      }
    });
  }

  @NotNull
  public static GeneralCommandLine makeFastpassCommand(Project project, @NotNull Collection<String> amendPart) {
    Optional<Path> fastpassPath = getFastpassPath(project);
    if(fastpassPath.isPresent()) {
      GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(project);
      commandLine.setExePath(fastpassPath.get().toString());
      commandLine.addParameters(new ArrayList<>(amendPart));
      return commandLine;
    } else {
      throw new PantsException("Could not find fastpass executable");
    }
  }

  @NotNull
  public static Optional<Path> getFastpassPath(Project project) {
    Optional<Path> root = PantsUtil.findPantsExecutable(project).map(VirtualFile::getParent).map(r -> Paths.get(r.getCanonicalPath()));
    if(!root.isPresent()){
      throw new PantsException("Not a Pants project");
    }

    Optional<Path> fastpassPath = root.map(r -> r.resolve("fastpass/bin/fastpass"));
    if(fastpassPath.map(p -> p.toFile().exists()).orElse(false)){
      return fastpassPath;
    } else {
      return Optional.empty();
    }
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

  public static CompletableFuture<Collection<PantsTargetAddress>> availableTargetsIn(Path path) {
      return CompletableFuture.supplyAsync( () -> {
        try {
          GeneralCommandLine cmd = PantsUtil.defaultCommandLine(path.toString());
          cmd.addParameters("list", path.toString() + "::");
          Process process = cmd.createProcess();
          BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
          List<String> message = new ArrayList<>();
          String line;
          while ((line = stdoutReader.readLine())!= null) {
            message.add(line);
          }
          process.waitFor();
          if (process.exitValue() == 0) {
            return message.stream().map(PantsTargetAddress::fromString).collect(Collectors.toSet());
          } else {
            throw new IOException(toString(process.getErrorStream()));
          }
        } catch (Throwable e) {
          throw new CompletionException(e);
        }
      });
  }


  public static CompletableFuture<Pair<PantsTargetAddress,Collection<PantsTargetAddress>>> validateAndGetDetails(
    VirtualFile pantsRoot,
    String targetString,
    Function<Path, CompletableFuture<Collection<PantsTargetAddress>>> fetcher
  ) {
    Optional<PantsTargetAddress> pantsTarget  = PantsTargetAddress.tryParse(targetString);
    if(!pantsTarget.isPresent()) {
      return failedFuture(new InvalidTargetException(targetString, "Malformed address"));
    }

    if(pantsRoot.findFileByRelativePath(pantsTarget.get().getPath().toString()) == null) {
      return failedFuture(new InvalidTargetException(targetString, "No such folder"));
    }

    return mapToSingleTargets(pantsTarget.get(), fetcher).thenApply(x -> Pair.create(pantsTarget.get(), x));
  }


  static CompletableFuture<Collection<PantsTargetAddress>> mapToSingleTargets(
    PantsTargetAddress targetAddress, Function<Path, CompletableFuture<Collection<PantsTargetAddress>>> fetcher
  ) {
    switch (targetAddress.getKind()){
      case SINGLE_TARGET: {
        CompletableFuture<Collection<PantsTargetAddress>> fut = fetcher.apply(targetAddress.getPath());
        return fut.thenApply(targets-> {
          if(targets.stream().noneMatch(target -> Objects.equals(target, targetAddress))) {
            throw new CompletionException(new InvalidTargetException(targetAddress.toAddressString(), "No such target"));
          } else {
            return Collections.singletonList(targetAddress);
          }
        });
      }
      case ALL_TARGETS_DEEP: {
        return fetcher.apply(targetAddress.getPath());
      }
      case ALL_TARGETS_FLAT: { return fetcher.apply(targetAddress.getPath())
        .thenApply(x -> x.stream()
          .filter(t -> t.getPath().equals(targetAddress.getPath()))
          .collect(Collectors.toSet()));
      }
    }
    return failedFuture(new InvalidTargetException(targetAddress.toAddressString(), "Invalid kind: " + targetAddress.getKind()));
  }

  /**
   *   Equivalent of JDK9 CompletableFuture::completeOnTimeout
   */
  public static <T> T completeOnTimeout(@NotNull CompletableFuture<T> c, int millis, @NotNull T fallback){
    if(!c.isDone()) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException ignored) {

      }
    }
    if(c.isDone()) {
      return c.join();
    } else {
      return fallback;
    }
  }

  /**
   * Replacement for JDK9's CompletableFuture::failedFuture
   */
  @NotNull
  public static  <T> CompletableFuture<T> failedFuture(Throwable ex) {
    return CompletableFuture.supplyAsync(() ->
                                         {
                                           throw new CompletionException(ex);
                                         });
  }

  public static CompletableFuture<Map<PantsTargetAddress, Collection<PantsTargetAddress>>> validateAndGetPreview(
    VirtualFile pantsRoot,
    Collection<String> targetStrings,
    Function<Path, CompletableFuture<Collection<PantsTargetAddress>>> fetcher
  ) {
    List<CompletableFuture<Pair<PantsTargetAddress, Collection<PantsTargetAddress>>>> futures =
      new HashSet<>(targetStrings).stream()
        .map(targetString -> FastpassUtils.validateAndGetDetails(pantsRoot, targetString, fetcher))
        .collect(Collectors.toList());
    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
      .thenApply(value -> futures.stream().map(CompletableFuture::join)
        .collect(Collectors.toMap(x -> x.getFirst(),
                                  x -> x.getSecond(),
                                  (col1, col2 ) -> col1
        ))
    );
  }

  /**
   * Instead of of JDK9's CompletableFuture::onExit
   */
  @NotNull
  public static CompletableFuture<Process> onExit(@NotNull Process process) {
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

  @NotNull
  public static Process fastpassProcess(GeneralCommandLine command, Path fastpassHome, Path pantsWorkspace)
    throws ExecutionException {
    return command
      .withWorkDirectory(pantsWorkspace.toFile())
      .withEnvironment("FASTPASS_HOME", fastpassHome.toString())
      .createProcess();
  }
}
