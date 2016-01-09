// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.task;

import com.google.gson.Gson;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.process.UnixProcessManager;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.task.AbstractExternalSystemTaskManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class PantsTaskManager extends AbstractExternalSystemTaskManager<PantsExecutionSettings> {
  public static final Map<String, String> goal2JvmOptionsFlag = ContainerUtil.newHashMap(
    Pair.create("test", "--jvm-test-junit-options"),
    Pair.create("run", "--jvm-run-jvm-options")
  );
  private final Map<ExternalSystemTaskId, Process> myCancellationMap = ContainerUtil.newConcurrentMap();


  @Override
  public void executeTasks(
    @NotNull final ExternalSystemTaskId id,
    @NotNull List<String> taskNames,
    @NotNull String projectPath,
    @Nullable PantsExecutionSettings settings,
    @NotNull List<String> vmOptions,
    @NotNull List<String> scriptParameters,
    @Nullable String debuggerSetup,
    @NotNull final ExternalSystemTaskNotificationListener listener
  ) throws ExternalSystemException {
    if (settings == null) {
      return;
    }
    projectPath = PantsTargetAddress.extractPath(projectPath);
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(projectPath);

    final String relativeProjectPath = PantsUtil.getRelativeProjectPath(commandLine.getWorkDirectory(), new File(projectPath));

    commandLine.addParameters(taskNames);
    if (!settings.getTargetNames().isEmpty()) {
      for (String targetName : settings.getTargetNames()) {
        commandLine.addParameter(relativeProjectPath + ":" + targetName);
      }
    }
    else {
      commandLine.addParameter(relativeProjectPath + File.separator + "::");
    }
    commandLine.addParameters(scriptParameters);
    for (String goal : taskNames) {
      final String jvmOptionsFlag = goal2JvmOptionsFlag.get(goal);
      if (jvmOptionsFlag == null) {
        continue;
      }
      for (String vmOption : vmOptions) {
        commandLine.addParameter(jvmOptionsFlag + "=" + vmOption);
      }
    }

    commandLine.addParameters("--no-colors");
    if (debuggerSetup != null) {
      if (taskNames.size() > 1) {
        throw new ExternalSystemException(PantsBundle.message("pants.error.multiple.tasks.for.debugging"));
      }
      final String goal = taskNames.iterator().next();
      final String jvmOptionsFlag = goal2JvmOptionsFlag.get(goal);
      if (jvmOptionsFlag == null) {
        throw new ExternalSystemException(PantsBundle.message("pants.error.cannot.debug.task", goal));
      }
      commandLine.addParameter(jvmOptionsFlag + "=" + debuggerSetup);
    }

    commandLine.addParameter(PantsUtil.getJdkParameter(ProjectJdkTable.getInstance()));

    listener.onTaskOutput(id, commandLine.getCommandLineString(PantsConstants.PANTS), true);
    try {
      final Process process = commandLine.createProcess();
      myCancellationMap.put(id, process);
      PantsUtil.getOutput(
        process,
        new ProcessAdapter() {
          @Override
          public void startNotified(ProcessEvent event) {
            super.startNotified(event);
            listener.onStart(id);
          }

          @Override
          public void onTextAvailable(ProcessEvent event, Key outputType) {
            super.onTextAvailable(event, outputType);
            listener.onTaskOutput(id, event.getText(), outputType == ProcessOutputTypes.STDOUT);
          }
        }
      );
    }
    catch (ExecutionException e) {
      throw new ExternalSystemException(e);
    }
    finally {
      myCancellationMap.remove(id);
    }
  }

  @Override
  public boolean cancelTask(@NotNull ExternalSystemTaskId id, @NotNull ExternalSystemTaskNotificationListener listener)
    throws ExternalSystemException {
    final Process process = myCancellationMap.get(id);
    UnixProcessManager.sendSignalToProcessTree(process, UnixProcessManager.SIGTERM);
    return true;
  }
}
