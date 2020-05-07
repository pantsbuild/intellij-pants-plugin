// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
  public static Stream<VirtualFile> pantsRoots(Module module) {
    return Stream.of(ModuleRootManager.getInstance(module).getSourceRoots()).flatMap (
      sourceRoot -> toStream(PantsUtil.findPantsExecutable(sourceRoot.getPath()).map(VirtualFile::getParent))
    );
  }

  public static CompletableFuture<Void> amendAll(@NotNull PantsBspData importData, Collection<String> newTargets, Project project)
    throws IOException, ExecutionException {
    List<String> amendPart = Arrays.asList(
      "amend",  "--no-bloop-exit", "--intellij", "--intellij-launcher", "echo",
      importData.getBspPath().getFileName().toString(),
      "--new-targets", String.join(",", newTargets)
    );
    GeneralCommandLine command = makeFastpassCommand(project, amendPart);
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

  public static CompletableFuture<Set<PantsTargetAddress>> selectedTargets(PantsBspData basePath, Project project) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        JsonReader reader = new JsonReader(new FileReader(basePath.getBspPath().resolve(Paths.get(".bsp", "bloop.json")).toFile()));
        Gson gson = new Gson();
        BloopConfig res = gson.fromJson(reader, BloopConfig.class);
        return res.getPantsTargets().stream().map(PantsTargetAddress::fromString).collect(Collectors.toSet());
      }
      catch (Throwable e) {
        throw new CompletionException(e);
      }
    });
  }


  private static List<String> coursierPart(){
    return Arrays.asList("launch", "org.scalameta:metals_2.12:0.8.4+114-d75e2293-SNAPSHOT",
                         "-r", "sonatype:snapshots",
                         "--main", "scala.meta.internal.pantsbuild.BloopPants",
                         "--"
    );
  }

  public static Path coursierPath() throws IOException {
    Path destination = Paths.get(System.getProperty("java.io.tmpdir"), "pants-plugin-coursier");
    if (!Files.exists(destination)) {
      URL url = new URL("https://git.io/coursier-cli");
      Files.copy(url.openConnection().getInputStream(), destination);
      destination.toFile().setExecutable(true);
    }
    return destination;
  }

  @NotNull
  public static GeneralCommandLine makeFastpassCommand(Project project, @NotNull Collection<String> amendPart) throws IOException {
    GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(project);
    String coursier = FastpassUtils.coursierPath().toString();
    commandLine.setExePath(coursier);
    commandLine.addParameters(coursierPart());
    commandLine.addParameters(new ArrayList<>(amendPart));
    return commandLine;
  }

  @NotNull
  private static Process fastpassProcess(GeneralCommandLine command, Path fastpassHome, Path pantsWorkspace) throws ExecutionException {
    return command
      .withWorkDirectory(pantsWorkspace.toFile())
      .withEnvironment("FASTPASS_HOME", fastpassHome.toString())
      .createProcess();
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
    return CompletableFuture.supplyAsync(
      () -> {
          try {
            GeneralCommandLine cmd = PantsUtil.defaultCommandLine(path.toString());
            cmd.addParameters("list", path.toString() + "::");
            Process process = cmd.createProcess();
            String stdout = toString(process.getInputStream());
            String[] list = stdout.equals("") ? new String[]{} : stdout.split("\n");
            return Stream.of(list).map(PantsTargetAddress::fromString).collect(Collectors.toSet());
          } catch (Throwable e) {
            throw new CompletionException(e);
          }
      });
  }


  public static CompletableFuture<Collection<PantsTargetAddress>> validateAndGetDetails(
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

    if(pantsTarget.get().getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP ||
       pantsTarget.get().getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_FLAT ) {
      return mapToSingleTarget(pantsTarget.get(), fetcher);
    } else {
      Path path = pantsTarget.get().getPath();
      CompletableFuture<Collection<PantsTargetAddress>> fut = fetcher.apply(path);
      return fut.thenApply(targets-> {
        if(targets.stream().noneMatch(target -> Objects.equals(target, pantsTarget.get()))) {
          throw new CompletionException(new InvalidTargetException(pantsTarget.toString(), "No such target"));
        } else {
          return Collections.singletonList(pantsTarget.get());
        }
      });
    }
  }


  static CompletableFuture<Collection<PantsTargetAddress>> mapToSingleTarget(
    PantsTargetAddress targetAddress, Function<Path, CompletableFuture<Collection<PantsTargetAddress>>> fetcher
  ) {
    switch (targetAddress.getKind()){
      case SINGLE_TARGET: {
        return CompletableFuture.completedFuture(Collections.singletonList(targetAddress));
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
    return null; // todo no null
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


  @NotNull
  static private CompletableFuture<Collection<PantsTargetAddress>> failedFuture(InvalidTargetException ex) {
    return CompletableFuture.supplyAsync(() ->
                                         {
                                           throw new CompletionException(ex);
                                         });
  }
}
