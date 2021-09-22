// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.twitter.intellij.pants.bsp.FastpassUtils.fastpassProcess;
import static com.twitter.intellij.pants.bsp.FastpassUtils.makeFastpassCommand;
import static com.twitter.intellij.pants.bsp.FastpassUtils.onExit;

public class AmendService {
  private CompletableFuture<Process> ongoingAmend;

  public AmendService() {
  }

  synchronized public Optional<CompletableFuture<Void>> amendAll(
    @NotNull PantsBspData importData,
    Collection<String> newTargets,
    Project project
  )
    throws IOException, ExecutionException {
    List<String> amendPart = Arrays.asList(
      "amend", "--no-bloop-exit", "--intellij", "--intellij-launcher", "echo",
      importData.getBspPath().getFileName().toString(),
      "--new-targets", String.join(",", newTargets)
    );

    if (!isAmendProcessOngoing()) {
      GeneralCommandLine command = makeFastpassCommand(project, amendPart);
      Process process = fastpassProcess(command, importData.getBspPath().getParent(), Paths.get(importData.getPantsRoot().getPath()));
      ongoingAmend = onExit(process);
      return Optional.of(ongoingAmend.thenAccept(__ -> {
      }));
    }
    else {
      return Optional.empty();
    }
  }

  synchronized boolean isAmendProcessOngoing() {
    return ongoingAmend != null && !ongoingAmend.isDone();
  }
}
