// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.process.CapturingAnsiEscapesAwareProcessHandler;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
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
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.codehaus.groovy.tools.shell.ExitNotification;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;

import javax.swing.*;
import java.util.Set;


public class PantsMakeBeforeRun extends ExternalSystemBeforeRunTaskProvider {
  public static final Key<ExternalSystemBeforeRunTask> ID = Key.create("Pants.BeforeRunTask");
  public Project myProject;

  private PantsOptions pantsOptions;
  private Boolean  hasTargetIdInExport = null;

  public PantsMakeBeforeRun(@NotNull Project project) {
    super(PantsConstants.SYSTEM_ID, project, ID);
    myProject = project;
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
    if (runConfiguration instanceof JUnitConfiguration || runConfiguration instanceof ScalaTestRunConfiguration) {
      ExternalSystemBeforeRunTask pantsTask = new ExternalSystemBeforeRunTask(ID, PantsConstants.SYSTEM_ID);
      pantsTask.setEnabled(true);
      return pantsTask;
    }
    else {
      return null;
    }
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
    // Force cached changes to disk
    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        ApplicationManager.getApplication().saveAll();
        ExternalSystemNotificationManager.getInstance(myProject).openMessageView(PantsConstants.SYSTEM_ID, NotificationSource.PROJECT_SYNC);
      }
    }, ModalityState.NON_MODAL);
    ExternalSystemNotificationManager.getInstance(myProject)
      .clearNotifications(NotificationSource.TASK_EXECUTION, PantsConstants.SYSTEM_ID);
    //final ExternalSystemTaskNotificationListener myListener
    //  = new ExternalSystemTaskNotificationListener();
    //ApplicationManager.getApplication().invokeLater(new Runnable() {
    //  @Override
    //  public void run() {
    //    ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "title") {
    //      public void run(@NotNull ProgressIndicator indicator) {
    //        indicator.setText("Prepare Pants compile...");
    //      }
    //    });
    //  }
    //});

    String dehydratedAddresses = null;
    /**
     * JUnit Test
     */
    if (configuration instanceof JUnitConfiguration) {
      JUnitConfiguration config = (JUnitConfiguration)configuration;
      Module[] targetModules = config.getModules();
      for (Module targetModule : targetModules) {
        dehydratedAddresses = targetModule.getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
        //addTargetAddressesToBuilder(builder, addresses);
      }
    }
    /**
     * Scala Test
     */
    else if (configuration instanceof ScalaTestRunConfiguration) {
      ScalaTestRunConfiguration config = (ScalaTestRunConfiguration)configuration;
      dehydratedAddresses = config.getModule().getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
      //addTargetAddressesToBuilder(builder, addresses);
    }
    if (dehydratedAddresses == null) {
      // no target address to compile
      return true;
    }
    Set<String> targetAddressesToCompile = PantsUtil.hydrateTargetAddresses(dehydratedAddresses);


    //final String recompileMessage;
    //if (targetAddressesToCompile.size() == 1) {
    //  recompileMessage = String.format("Compiling %s...", targetAddressesToCompile.iterator().next());
    //}
    //else {
    //  recompileMessage = String.format("Compiling %s targets", targetAddressesToCompile.size());
    //}
    //context.processMessage(new ProgressMessage(recompileMessage));
    //context.processMessage(new CompilerMessage(PantsConstants.PLUGIN, BuildMessage.Kind.INFO, recompileMessage));
    VirtualFile pantsExe = PantsUtil.findPantsExecutable(myProject.getProjectFile());
    if (pantsExe == null) {
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
    // Find out whether "export-classpath-use-old-naming-style" exists
    final boolean hasExportClassPathNamingStyle = pantsOptions.hasExportClassPathNamingStyle();

    // "export-classpath-use-old-naming-style" is soon to be removed.
    // so add this flag only if target id is exported and this flag supported.
    if (hasExportClassPathNamingStyle && hasTargetIdInExport) {
      commandLine.addParameters("--no-export-classpath-use-old-naming-style");
    }

    commandLine.addParameters(PantsConstants.PANTS_OPTION_NO_COLORS, "export-classpath", "compile");
    for (String targetAddress : targetAddressesToCompile) {
      commandLine.addParameter(targetAddress);
    }

    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        NotificationData notification =
          new NotificationData(PantsConstants.PANTS, commandLine.getCommandLineString(), NotificationCategory.INFO,
                               NotificationSource.TASK_EXECUTION
          );
        ExternalSystemNotificationManager.getInstance(myProject).showNotification(PantsConstants.SYSTEM_ID, notification);
      }
    }, ModalityState.NON_MODAL);


    //final JpsProject jpsProject = context.getProjectDescriptor().getProject();
    //final JpsPantsProjectExtension pantsProjectExtension =
    //  PantsJpsProjectExtensionSerializer.findPantsProjectExtension(jpsProject);
    //if (pantsProjectExtension != null && pantsProjectExtension.isUseIdeaProjectJdk()) {
    //  try {
    //    commandLine.addParameter(PantsUtil.getJvmDistributionPathParameter(PantsUtil.getJdkPathFromExternalBuilder(jpsProject)));
    //  }
    //  catch (Exception e) {
    //    throw new ProjectBuildException(e);
    //  }
    //}

    final Process process;
    try {
      process = commandLine.createProcess();
      //context.processMessage(new CompilerMessage(PantsConstants.PLUGIN, BuildMessage.Kind.INFO, commandLine.getCommandLineString()));
    }
    catch (ExecutionException e) {
      return false;
    }

    final CapturingProcessHandler processHandler = new CapturingAnsiEscapesAwareProcessHandler(process);


    processHandler.addProcessListener(
      new ProcessAdapter() {
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          super.onTextAvailable(event, outputType);
          //final PantsOutputMessage message = PantsOutputMessage.parseCompilerMessage(event.getText());
          //Messages.show(myProject,event.getText(),"hi");
          String output = event.getText();
          if (StringUtil.isEmptyOrSpaces(output)) {
            return;
          }
          NotificationCategory cat = NotificationCategory.INFO;
          //if (outputType == ProcessOutputTypes.SYSTEM) {
          //  cat = NotificationCategory.INFO;
          //}
          if (output.contains("[warn]")) {
            cat = NotificationCategory.WARNING;
          }
          else if (output.contains("[error]")) {
            cat = NotificationCategory.ERROR;
          }
          else {
            int x = 5;
          }
          NotificationData notification = new NotificationData(PantsConstants.PANTS, output, cat, NotificationSource.TASK_EXECUTION);
          ExternalSystemNotificationManager.getInstance(myProject).showNotification(PantsConstants.SYSTEM_ID, notification);
        }
      }
    );

    //ApplicationManager.getApplication().invokeLater(new Runnable() {
    //  @Override
    //  public void run() {
    //    NotificationGroup.balloonGroup(PantsConstants.PANTS);
    //    Notifications.Bus.register(PantsConstants.PANTS, NotificationDisplayType.BALLOON);
    //    Notifications.Bus.notify(new Notification(PantsConstants.PANTS, "Title", "Description", NotificationType.INFORMATION));
    //  }
    //});
    //checkCompileCancellationInBackground(context, process, processHandler);


    //ApplicationManager.getApplication().invokeLater(new Runnable() {
    //  @Override
    //  public void run() {
    //    ProgressManager.getInstance().run(new Task.Backgroundable(myProject, "title") {
    //      public void run(@NotNull ProgressIndicator indicator) {
    //        //indicator.start();
    //        indicator.setText("Pants is compiling some stuff");
    //        while (!processHandler.isProcessTerminated()) {
    //          try {
    //            Thread.sleep(1000);
    //          }
    //          catch (InterruptedException e) {
    //            e.printStackTrace();
    //          }
    //        }
    //        //indicator.
    //        //indicator.setFraction(0.5);  // halfway done
    //
    //      }
    //    });
    //  }
    //});
    processHandler.runProcess();

    final boolean success = process.exitValue() == 0;
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        String message = success ? "Pants compile succeeded." : "Pants compile failed.";
        NotificationType type = success? NotificationType.INFORMATION : NotificationType.ERROR;
        Notification start = new Notification(PantsConstants.PANTS, "Compile message", message, type);
        Notifications.Bus.notify(start);
      }
    });


    return success;
  }
}
