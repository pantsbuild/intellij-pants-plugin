// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.task;

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
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

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
    final GeneralCommandLine commandLine = constructCommandLine(taskNames, projectPath, settings, vmOptions, scriptParameters, debuggerSetup);
    if (commandLine == null) return;

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
      // Sync files as generated sources may have changed after `pants test` called
      PantsUtil.synchronizeFiles();
    }
  }

  @Nullable
  public GeneralCommandLine constructCommandLine(
    @NotNull List<String> taskNames,
    @NotNull String projectPath,
    @Nullable PantsExecutionSettings settings,
    @NotNull List<String> vmOptions,
    @NotNull List<String> scriptParameters,
    @Nullable String debuggerSetup
  ) {
    if (settings == null) {
      return null;
    }
    projectPath = PantsTargetAddress.extractPath(projectPath);
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(projectPath);

    /**
     * Global options section.
     */
    if (debuggerSetup != null) {
      if (taskNames.size() > 1) {
        throw new ExternalSystemException(PantsBundle.message("pants.error.multiple.tasks.for.debugging"));
      }
      commandLine.addParameter(PantsConstants.PANTS_CLI_OPTION_NO_TEST_JUNIT_TIMEOUTS);
      final String goal = taskNames.iterator().next();
      final String jvmOptionsFlag = goal2JvmOptionsFlag.get(goal);
      if (jvmOptionsFlag == null) {
        throw new ExternalSystemException(PantsBundle.message("pants.error.cannot.debug.task", goal));
      }
      commandLine.addParameter(jvmOptionsFlag + "=" + debuggerSetup);
    }
    if (settings.isUseIdeaProjectJdk()) {
      try{
        commandLine.addParameter(PantsUtil.getJvmDistributionPathParameter(PantsUtil.getJdkPathFromIntelliJCore()));
      }
      catch(Exception e){
        throw new ExternalSystemException(e);
      }
    }
    /**
     * Goals.
     */
    commandLine.addParameters(taskNames);
    // Appending VM options.
    for (String goal : taskNames) {
      final String jvmOptionsFlag = goal2JvmOptionsFlag.get(goal);
      if (jvmOptionsFlag == null) {
        continue;
      }
      for (String vmOption : vmOptions) {
        commandLine.addParameter(jvmOptionsFlag + "=" + vmOption);
      }
    }
    /**
     * Script parameters section including targets and options.
     */
    commandLine.addParameters(scriptParameters);
    return commandLine;
  }

  @Override
  public boolean cancelTask(@NotNull ExternalSystemTaskId id, @NotNull ExternalSystemTaskNotificationListener listener)
    throws ExternalSystemException {
    final Process process = myCancellationMap.get(id);
    UnixProcessManager.sendSignalToProcessTree(process, UnixProcessManager.SIGTERM);
    return true;
  }
}
