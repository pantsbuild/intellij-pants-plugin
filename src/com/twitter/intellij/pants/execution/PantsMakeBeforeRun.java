// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.process.CapturingAnsiEscapesAwareProcessHandler;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager;
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.externalSystem.service.notification.NotificationSource;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.model.PantsOptions;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * PantsMakeBeforeRun creates a custom Make process `PantsCompile` to replace IntelliJ's default Make process whenever a new configuration
 * is added under a pants project via {@link PantsMakeBeforeRun#replaceDefaultMakeWithPantsMake}, so the time to launch pants is minimized.
 * <p/>
 * Motivation: By default, IntelliJ's Make process is invoked before any JUnit/Scala/Application run which has unnecessary (for Pants)
 * long steps to scan the entire project to assist external builders' incremental compile.
 */
public class PantsMakeBeforeRun extends ExternalSystemBeforeRunTaskProvider {

  public static final Key<ExternalSystemBeforeRunTask> ID = Key.create("Pants.BeforeRunTask");

  public PantsMakeBeforeRun(@NotNull Project project) {
    super(PantsConstants.SYSTEM_ID, project, ID);
  }

  public static void replaceDefaultMakeWithPantsMake(@NotNull Project project, @NotNull RunnerAndConfigurationSettings settings) {
    RunManager runManager = RunManager.getInstance(project);
    if (!(runManager instanceof RunManagerImpl)) {
      return;
    }
    RunManagerImpl runManagerImpl = (RunManagerImpl) runManager;
    RunConfiguration runConfiguration = settings.getConfiguration();

    Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(project);

    /**
     /**
     * Scala related run/test configuration inherit {@link AbstractTestRunConfiguration}
     */
    if (runConfiguration instanceof AbstractTestRunConfiguration) {
      if (buildRoot.isPresent()) {
        ((AbstractTestRunConfiguration) runConfiguration).setWorkingDirectory(buildRoot.get().getPath());
      }
    }
    /**
     * JUnit, Application, etc configuration inherit {@link CommonProgramRunConfigurationParameters}
     */
    else if (runConfiguration instanceof CommonProgramRunConfigurationParameters) {
      if (buildRoot.isPresent()) {
        ((CommonProgramRunConfigurationParameters) runConfiguration).setWorkingDirectory(buildRoot.get().getPath());
      }
    }
    /**
     * If neither applies (e.g. Pants or pytest configuration), do not continue.
     */
    else {
      return;
    }

    /**
     * Every time a new configuration is created, 'Make' is by default added to the "Before launch" tasks.
     * Therefore we want to overwrite it with {@link PantsMakeBeforeRun}.
     */
    BeforeRunTask pantsMakeTask = new ExternalSystemBeforeRunTask(ID, PantsConstants.SYSTEM_ID);
    pantsMakeTask.setEnabled(true);
    runManagerImpl.setBeforeRunTasks(runConfiguration, Collections.singletonList(pantsMakeTask), false);
  }

  @Override
  public String getName() {
    return "PantsCompile";
  }

  @Override
  public String getDescription(ExternalSystemBeforeRunTask task) {
    return "PantsCompile";
  }

  @Nullable
  @Override
  public ExternalSystemBeforeRunTask createTask(RunConfiguration runConfiguration) {
    return new ExternalSystemBeforeRunTask(ID, PantsConstants.SYSTEM_ID);
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return PantsIcons.Icon;
  }

  @Override
  public boolean canExecuteTask(
    RunConfiguration configuration, ExternalSystemBeforeRunTask beforeRunTask
  ) {
    return true;
  }

  @Override
  public boolean executeTask(
    final DataContext context,
    RunConfiguration configuration,
    ExecutionEnvironment env,
    ExternalSystemBeforeRunTask beforeRunTask
  ) {
    Project currentProject = configuration.getProject();
    prepareIDE(currentProject);
    Set<String> targetAddressesToCompile = PantsUtil.filterGenTargets(getTargetAddressesToCompile(configuration));
    Pair<Boolean, Optional<String>> result = executeTask(currentProject, targetAddressesToCompile, false);
    return result.getFirst();
  }

  public Pair<Boolean, Optional<String>> executeTask(Project project) {
    return executeTask(project, getTargetAddressesToCompile(ModuleManager.getInstance(project).getModules()), false);
  }

  public Pair<Boolean, Optional<String>> executeTask(@NotNull Module [] modules) {
    if (modules.length == 0) {
      return Pair.create(false, null);
    }
    return executeTask(modules[0].getProject(), getTargetAddressesToCompile(modules), false);
  }

  public Pair<Boolean, Optional<String>> executeTask(Project currentProject, Set<String> targetAddressesToCompile, boolean useCleanAll) {
    prepareIDE(currentProject);
    if (targetAddressesToCompile.isEmpty()) {
      showPantsMakeTaskMessage("No target found in configuration.", NotificationCategory.INFO, currentProject);
      return Pair.create(true, null);
    }

    Optional<VirtualFile> pantsExecutable1 = PantsUtil.findPantsExecutable(currentProject);
    if (!pantsExecutable1.isPresent()) {
      return Pair.create(
        false,
        Optional.of(
          PantsBundle.message("pants.error.no.pants.executable.by.path", currentProject.getProjectFilePath())
        )
      );
    }
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(pantsExecutable1.get().getPath());

    showPantsMakeTaskMessage("Checking Pants options...", NotificationCategory.INFO, currentProject);
    Optional<PantsOptions> pantsOptional = PantsOptions.getPantsOptions(currentProject);
    if (!pantsOptional.isPresent()) {
      showPantsMakeTaskMessage("Pants Options not found.", NotificationCategory.ERROR, currentProject);
      return Pair.create(false, null);
    }

    PantsOptions pantsOptions = pantsOptional.get();

    /* Global options section. */
    commandLine.addParameter(PantsConstants.PANTS_CLI_OPTION_NO_COLORS);

    if (useCleanAll) {
      commandLine.addParameter("clean-all");
      if (pantsOptions.supportsAsyncCleanAll()) {
        commandLine.addParameter(PantsConstants.PANTS_CLI_OPTION_ASYNC_CLEAN_ALL);
      }
    }
    if (pantsOptions.supportsManifestJar()) {
      commandLine.addParameter(PantsConstants.PANTS_CLI_OPTION_EXPORT_CLASSPATH_MANIFEST_JAR);
    }

    PantsSettings settings = PantsSettings.getInstance(currentProject);
    if (settings.isUseIdeaProjectJdk()) {
      try {
        commandLine.addParameter(PantsUtil.getJvmDistributionPathParameter(PantsUtil.getJdkPathFromIntelliJCore()));
      }
      catch (Exception e) {
        showPantsMakeTaskMessage(e.getMessage(), NotificationCategory.ERROR, currentProject);
        return Pair.create(false, null);
      }
    }

    /* Goals and targets section. */
    commandLine.addParameters("export-classpath", "compile");
    for (String targetAddress : targetAddressesToCompile) {
      commandLine.addParameter(targetAddress);
    }

    final Process process;
    try {
      process = commandLine.createProcess();
    }
    catch (ExecutionException e) {
      showPantsMakeTaskMessage(e.getMessage(), NotificationCategory.ERROR, currentProject);
      return Pair.create(false, null);
    }

    final CapturingProcessHandler processHandler = new CapturingAnsiEscapesAwareProcessHandler(process, commandLine.getCommandLineString());
    addMessageHandler(processHandler, currentProject);
    final List<String> output = new ArrayList<>();
    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void onTextAvailable(ProcessEvent event, Key outputType) {
        super.onTextAvailable(event, outputType);
        output.add(event.getText());
      }
    });
    processHandler.runProcess();

    final boolean success = process.exitValue() == 0;
    notifyCompileResult(success);
    // Sync files as generated sources may have changed after Pants compile.
    PantsUtil.synchronizeFiles();
    String finalOutString = String.join("", output);
    Pair<Boolean, Optional<String>> result = Pair.create(success, Optional.of(finalOutString));
    return result;
  }

  private void notifyCompileResult(final boolean success) {
  /* Show pop up notification about pants compile result. */
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        String message = success ? "Pants compile succeeded." : "Pants compile failed.";
        NotificationType type = success ? NotificationType.INFORMATION : NotificationType.ERROR;
        Notification start = new Notification(PantsConstants.PANTS, "Compile message", message, type);
        Notifications.Bus.notify(start);
      }
    });
  }

  private void prepareIDE(Project project) {
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        /* Clear message window. */
        ExternalSystemNotificationManager.getInstance(project)
          .clearNotifications(NotificationSource.TASK_EXECUTION, PantsConstants.SYSTEM_ID);
        /* Force cached changes to disk. */
        FileDocumentManager.getInstance().saveAllDocuments();
        project.save();
      }
    }, ModalityState.NON_MODAL);

    ExternalSystemNotificationManager.getInstance(project).openMessageView(PantsConstants.SYSTEM_ID, NotificationSource.TASK_EXECUTION);
  }

  private void addMessageHandler(CapturingProcessHandler processHandler, Project project) {
    processHandler.addProcessListener(
      new ProcessAdapter() {
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          super.onTextAvailable(event, outputType);
          String output = event.getText();
          if (StringUtil.isEmptyOrSpaces(output)) {
            return;
          }
          NotificationCategory notificationCategory = NotificationCategory.INFO;
          if (output.contains("[warn]")) {
            notificationCategory = NotificationCategory.WARNING;
          }
          else if (output.contains("[error]")) {
            notificationCategory = NotificationCategory.ERROR;
          }

          NotificationData notification =
            new NotificationData(PantsConstants.PANTS, output, notificationCategory, NotificationSource.TASK_EXECUTION);
          ExternalSystemNotificationManager.getInstance(project).showNotification(PantsConstants.SYSTEM_ID, notification);
        }
      }
    );
  }

  @NotNull
  private Set<String> getTargetAddressesToCompile(RunConfiguration configuration) {
    /* JUnit, Application, Scala runs */
    if (configuration instanceof RunProfileWithCompileBeforeLaunchOption) {
      RunProfileWithCompileBeforeLaunchOption config = (RunProfileWithCompileBeforeLaunchOption) configuration;
      Module[] targetModules = config.getModules();
      return getTargetAddressesToCompile(targetModules);
    }
    else {
      return Collections.emptySet();
    }
  }

  @NotNull
  private Set<String> getTargetAddressesToCompile(Module[] targetModules) {
    if (targetModules.length == 0) {
      return Collections.emptySet();
    }
    Set<String> result = new HashSet<>();
    for (Module targetModule : targetModules) {
      result.addAll(PantsUtil.getNonGenTargetAddresses(targetModule));
    }
    return result;
  }

  private void showPantsMakeTaskMessage(String message, NotificationCategory type, Project project) {
    NotificationData notification =
      new NotificationData(PantsConstants.PANTS, message, type, NotificationSource.TASK_EXECUTION);
    ExternalSystemNotificationManager.getInstance(project).showNotification(PantsConstants.SYSTEM_ID, notification);
  }
}
