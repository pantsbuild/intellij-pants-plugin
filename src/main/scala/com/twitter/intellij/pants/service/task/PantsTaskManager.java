// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.task;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.process.UnixProcessManager;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.task.ExternalSystemTaskManager;
import com.intellij.openapi.util.Key;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListener;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListenerManager;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.intellij.openapi.externalSystem.rt.execution.ForkedDebuggerHelper.JVM_DEBUG_SETUP_PREFIX;
import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunnableState.BUILD_PROCESS_DEBUGGER_PORT_KEY;

public class PantsTaskManager implements ExternalSystemTaskManager<PantsExecutionSettings> {

  private static final Map<String, String> goal2JvmOptionsFlag = ImmutableMap.<String, String>builder()
    .put("test", "--jvm-test-junit-options")
    .put("run", "--jvm-run-jvm-options")
    .build();

  private final Map<ExternalSystemTaskId, Process> myCancellationMap = ContainerUtil.newConcurrentMap();

  @Override
  public void executeTasks(
    @NotNull ExternalSystemTaskId id,
    @NotNull List<String> taskNames,
    @NotNull String projectPath,
    @Nullable PantsExecutionSettings settings,
    @Nullable String jvmAgentSetup,
    @NotNull ExternalSystemTaskNotificationListener listener
  ) throws ExternalSystemException {
    PantsExternalMetricsListenerManager.getInstance().logTestRunner(PantsExternalMetricsListener.TestRunnerType.PANTS_RUNNER);
    if (settings == null) {
      return;
    }

    GeneralCommandLine commandLine = constructCommandLine(taskNames, projectPath, settings);
    if (commandLine == null) {
      return;
    }

    listener.onTaskOutput(id, commandLine.getCommandLineString(PantsConstants.PANTS), true);
    try {
      final Process process = commandLine.createProcess();
      List<String> textOutputs = Lists.newArrayList();
      myCancellationMap.put(id, process);
      PantsUtil.getCmdOutput(
        process,
        commandLine.getCommandLineString(), new ProcessAdapter() {
          @Override
          public void startNotified(@NotNull ProcessEvent event) {
            super.startNotified(event);
            listener.onStart(id, commandLine.getWorkDirectory().getPath());
          }

          @Override
          public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            super.onTextAvailable(event, outputType);
            textOutputs.add(event.getText());
            listener.onTaskOutput(id, event.getText(), outputType == ProcessOutputTypes.STDOUT);
          }
        }
      );
      int exitCode = process.waitFor();

      // https://github.com/JetBrains/intellij-community/blob/master/platform/external-system-impl/src/com/intellij/openapi/externalSystem/service/remote/wrapper/ExternalSystemTaskManagerWrapper.java#L54-L57
      // explicitly expects ExternalSystemException for the task to fail, so we are now throwing it upon non-zero exit.
      if (exitCode != 0) {
        throw new ExecutionException(String.join("", textOutputs));
      }
    }
    catch (ExecutionException | InterruptedException e) {
      throw new ExternalSystemException(e);
    }
    finally {
      myCancellationMap.remove(id);
      // Sync files as generated sources may have changed after `pants test` called
      PantsUtil.synchronizeFiles();
    }
  }

  /**
   * Tries to estimate if the command line was created with PantsPythonTestRunConfigurationProducer
   * so it's a python test case run
   *
   * The result is not guaranteed to be correct, so the methods should be used only in non-critical code
   * (for example to reduce the number of non-critical notifications)
   */
  private boolean mayBePythonTestCommandLine(GeneralCommandLine commandLine) {
    return commandLine.getCommandLineString().contains(PantsConstants.PANTS_CLI_OPTION_PYTEST);
  }

  @VisibleForTesting
  protected static void setupDebuggerSettings(PantsExecutionSettings settings) {
    Optional.ofNullable(settings.getUserData(BUILD_PROCESS_DEBUGGER_PORT_KEY))
      .filter(port -> port > 0)
      .map(port -> JVM_DEBUG_SETUP_PREFIX + port)
      .ifPresent(settings::withVmOption);
  }

  @Nullable
  public GeneralCommandLine constructCommandLine(
    @NotNull List<String> taskNames,
    @NotNull String projectPath,
    @Nullable PantsExecutionSettings settings
  ) {
    if (settings == null) {
      return null;
    }
    projectPath = PantsTargetAddress.extractPath(projectPath).get();
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(projectPath);

    setupDebuggerSettings(settings);
    boolean debugEnabled = settings.getJvmArguments().stream()
      .anyMatch(jvmOpt -> jvmOpt.startsWith(JVM_DEBUG_SETUP_PREFIX));

    /**
     * Global options section.
     */
    if (debugEnabled) {
      if (taskNames.size() > 1) {
        throw new ExternalSystemException(PantsBundle.message("pants.error.multiple.tasks.for.debugging"));
      }
      final String goal = taskNames.iterator().next();
      if (!goal2JvmOptionsFlag.containsKey(goal)) {
        throw new ExternalSystemException(PantsBundle.message("pants.error.cannot.debug.task", goal));
      }

      commandLine.addParameter(PantsConstants.PANTS_CLI_OPTION_NO_TEST_JUNIT_TIMEOUTS);
    }
    if (settings.isUseIdeaProjectJdk()) {
      try {
        commandLine.addParameter(PantsUtil.getJvmDistributionPathParameter(PantsUtil.getJdkPathFromIntelliJCore()));
      }
      catch (Exception e) {
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
      for (String vmOption : settings.getJvmArguments()) {
        commandLine.addParameter(jvmOptionsFlag + "=" + vmOption);
      }
    }
    /**
     * Script parameters section including targets and options.
     */
    commandLine.addParameters(settings.getArguments());
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
