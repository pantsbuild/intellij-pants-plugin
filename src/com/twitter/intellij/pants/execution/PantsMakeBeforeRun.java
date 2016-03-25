// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JUnitConfiguration;
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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.model.PantsOptions;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;

import javax.swing.*;
import java.util.Set;


public class PantsMakeBeforeRun extends ExternalSystemBeforeRunTaskProvider {

  public static final Key<ExternalSystemBeforeRunTask> ID = Key.create("Pants.BeforeRunTask");
  public Project myProject;
  private PantsOptions pantsOptions;
  private Boolean hasTargetIdInExport = null;

  public PantsMakeBeforeRun(@NotNull Project project) {
    super(PantsConstants.SYSTEM_ID, project, ID);
    myProject = project;
  }

  public static boolean needPantsMakeBeforeRun(RunConfiguration runConfiguration) {
    return runConfiguration instanceof JUnitConfiguration ||
           runConfiguration instanceof ScalaTestRunConfiguration ||
           runConfiguration instanceof ApplicationConfiguration;
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
    //if (PantsUtil.isPantsProject(myProject)) {
    ExternalSystemBeforeRunTask pantsTask = new ExternalSystemBeforeRunTask(ID, PantsConstants.SYSTEM_ID);
    //pantsTask.setEnabled(true);
    return pantsTask;
    //}
    //return null;
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
    /**
     * Clear message window.
     */
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        ExternalSystemNotificationManager.getInstance(myProject)
          .clearNotifications(NotificationSource.TASK_EXECUTION, PantsConstants.SYSTEM_ID);
      }
    }, ModalityState.NON_MODAL);

    /**
     * Force cached changes to disk.
     */
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().saveAll();
      }
    }, ModalityState.NON_MODAL);

    ExternalSystemNotificationManager.getInstance(myProject).openMessageView(PantsConstants.SYSTEM_ID, NotificationSource.TASK_EXECUTION);

    PantsSettings settings = PantsSettings.getInstance(myProject);

    Set<String> targetAddressesToCompile = getTargetAddressesToCompile(configuration);

    if (targetAddressesToCompile == null) {
      // no target address to compile
      return true;
    }

    VirtualFile pantsExe = PantsUtil.findPantsExecutable(myProject.getProjectFile());
    if (pantsExe == null) {
      showPantsMakeTaskMessage("Pants executable not found", NotificationCategory.ERROR);
      return false;
    }
    String pantsExecutable = pantsExe.getPath();

    if (pantsOptions == null) {
      pantsOptions = new PantsOptions(pantsExecutable);
    }

    if (hasTargetIdInExport == null) {
      hasTargetIdInExport = PantsUtil.hasTargetIdInExport(pantsExecutable);
    }

    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(pantsExecutable);

    /* Global options section. */

    // Find out whether "export-classpath-use-old-naming-style" exists
    final boolean hasExportClassPathNamingStyle = pantsOptions.hasExportClassPathNamingStyle();

    // "export-classpath-use-old-naming-style" is soon to be removed.
    // so add this flag only if target id is exported and this flag supported.
    if (hasExportClassPathNamingStyle && hasTargetIdInExport) {
      commandLine.addParameters("--no-export-classpath-use-old-naming-style");
    }

    if (settings.isUseIdeaProjectJdk()) {
      try {
        commandLine.addParameter(PantsUtil.getJvmDistributionPathParameter(PantsUtil.getJdkPathFromIntelliJCore()));
      }
      catch (Exception e) {
        showPantsMakeTaskMessage(e.getMessage(), NotificationCategory.ERROR);
        return false;
      }
    }

    /* Goals and targets section. */
    commandLine.addParameters(PantsConstants.PANTS_OPTION_NO_COLORS, "export-classpath", "compile");
    for (String targetAddress : targetAddressesToCompile) {
      commandLine.addParameter(targetAddress);
    }
    /* Shell off. */
    showPantsMakeTaskMessage(commandLine.getCommandLineString(), NotificationCategory.INFO);

    final Process process;
    try {
      process = commandLine.createProcess();
    }
    catch (ExecutionException e) {
      showPantsMakeTaskMessage(e.getMessage(), NotificationCategory.ERROR);
      return false;
    }

    final CapturingProcessHandler processHandler = new CapturingAnsiEscapesAwareProcessHandler(process);
    addMessageHandler(processHandler);
    processHandler.runProcess();
    final boolean success = process.exitValue() == 0;

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
    return success;
  }

  private void addMessageHandler(CapturingProcessHandler processHandler) {
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
          ExternalSystemNotificationManager.getInstance(myProject).showNotification(PantsConstants.SYSTEM_ID, notification);
        }
      }
    );
  }

  @Nullable
  private Set<String> getTargetAddressesToCompile(RunConfiguration configuration) {
    if (!needPantsMakeBeforeRun(configuration)) {
      return null;
    }

    String dehydratedAddresses = null;
    /* JUnit Test */
    if (configuration instanceof JUnitConfiguration) {
      JUnitConfiguration config = (JUnitConfiguration)configuration;
      Module[] targetModules = config.getModules();
      for (Module targetModule : targetModules) {
        dehydratedAddresses = targetModule.getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
      }
    }
    /* Scala Test */
    else if (configuration instanceof ScalaTestRunConfiguration) {
      ScalaTestRunConfiguration config = (ScalaTestRunConfiguration)configuration;
      dehydratedAddresses = config.getModule().getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
    }
    /* Application run */
    else if (configuration instanceof ApplicationConfiguration) {
      ApplicationConfiguration config = (ApplicationConfiguration)configuration;
      Module[] targetModules = config.getModules();
      for (Module targetModule : targetModules) {
        dehydratedAddresses = targetModule.getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
      }
    }
    if (dehydratedAddresses == null) {
      return null;
    }
    return PantsUtil.hydrateTargetAddresses(dehydratedAddresses);
  }

  private void showPantsMakeTaskMessage(String message, NotificationCategory type) {
    NotificationData notification =
      new NotificationData(PantsConstants.PANTS, message, type, NotificationSource.TASK_EXECUTION);
    ExternalSystemNotificationManager.getInstance(myProject).showNotification(PantsConstants.SYSTEM_ID, notification);
  }
}
