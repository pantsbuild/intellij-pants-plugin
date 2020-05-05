// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.google.gson.Gson;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.notification.EventLog;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class FastpassUpdater {

  private static final Logger LOG = Logger.getInstance(FastpassUpdater.class);

  public static final String FASTPASS_PATH = "/opt/twitter_mde/bin/fastpass";
  public static final String BLOOP_PATH = "/opt/twitter_mde/bin/bloop";

  private static final String NOTIFICATION_TITLE = "Fastpass update available";
  public static final String NOTIFICATION_HREF = "fastpass-update";

  private static boolean initialized = false;

  private static class BloopSettings {
    public List<String> refreshProjectsCommand;
  }

  private static class FastpassData {
    public String version;
    public String projectName;

    public FastpassData(String version, String projectName) {
      this.version = version;
      this.projectName = projectName;
    }
  }

  public static final class Action extends AnAction {

    public static final String DIALOG_TITLE = "Fastpass Update";

    @Override
    public void update(@NotNull AnActionEvent e) {
      boolean show = fastpassBinaryExists();
      e.getPresentation().setEnabledAndVisible(show);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (fastpassBinaryExists()) {
        systemVersion(FASTPASS_PATH)
          .ifPresent(systemVersion -> extractFastpassData(project)
            .ifPresent(data -> {
              if (!data.version.equals(systemVersion)) {
                String message = "Do you want to update fastpass to version: " + systemVersion + "?";
                int answer = Messages.showYesNoDialog(project, message, DIALOG_TITLE, null);
                if (answer == Messages.YES) {
                  updateFastpassVersion(project, data);
                }
              } else {
                Messages.showInfoMessage(project, "Fastpass is already up to date", DIALOG_TITLE);
              }
            }));
      }
    }

    private boolean fastpassBinaryExists() {
      VirtualFile fastpassBinary = LocalFileSystem.getInstance().findFileByPath(FASTPASS_PATH);
      return fastpassBinary != null && fastpassBinary.exists();
    }
  }

  public static void initialize(Project project) {
    if (PantsUtil.isBspProject(project) && !initialized) {
      synchronized (FastpassUpdater.class) {
        if (!initialized) {
          ScheduledExecutorService timer = Executors.newScheduledThreadPool(1);
          TimeUnit timeUnit = TimeUnit.SECONDS;
          timer.scheduleWithFixedDelay(FastpassUpdater::checkForFastpassUpdates,
                                       timeUnit.convert(1, TimeUnit.MINUTES),
                                       timeUnit.convert(1, TimeUnit.DAYS),
                                       timeUnit);
          initialized = true;
        }
      }
    }
  }

  private static void checkForFastpassUpdates() {
    VirtualFile fastpassBinary = LocalFileSystem.getInstance().findFileByPath(FASTPASS_PATH);
    if (fastpassBinary != null && fastpassBinary.exists()) {
      systemVersion(fastpassBinary.getPath())
        .ifPresent(systemVersion -> allOpenBspProjects()
          .forEach(project -> extractFastpassData(project)
            .ifPresent(data -> {
              if (!data.version.equals(systemVersion)) {
                showUpdateNotification(project, systemVersion, data);
              }
            })));
    }
  }

  private static void showUpdateNotification(Project project, String systemVersion, FastpassData data) {
    if (!hasNotification(project)) {
      Notification notification = new NotificationGroup(PantsConstants.PANTS, NotificationDisplayType.STICKY_BALLOON, true)
        .createNotification(
          NOTIFICATION_TITLE,
          "<a href='" + NOTIFICATION_HREF + "'>Update</a> fastpass to version: " + systemVersion,
          NotificationType.INFORMATION,
          new NotificationListener.Adapter() {
            @Override
            protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
              if (NOTIFICATION_HREF.equals(e.getDescription())) {
                updateFastpassVersion(project, data);
              }
              notification.expire();
            }
          }
        );

      notification.notify(project);
    }
  }

  private static boolean hasNotification(Project project) {
    ArrayList<Notification> notifications = EventLog.getLogModel(project).getNotifications();
    return notifications.stream().anyMatch(s -> s.getTitle().equals(NOTIFICATION_TITLE));
  }

  private static void updateFastpassVersion(Project project, FastpassData data) {
    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Updating fastpass version") {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        indicator.setText("Exiting bloop");
        exitBloop();
        indicator.setText("Running fastpass refresh");
        boolean ok = runFastpassRefresh(data, project);
        if (ok) {
          indicator.setText("Refreshing BSP project after fastpass update");
          PantsUtil.refreshAllProjects(project);
        }
      }
    });
  }

  private static void exitBloop() {
    try {
      new ProcessBuilder(BLOOP_PATH, "exit").start().waitFor();
    }
    catch (Exception e) {
      LOG.warn(e);
    }
  }

  private static boolean runFastpassRefresh(FastpassData data, Project project) {
    try {
      GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(project);
      commandLine.setExePath(FASTPASS_PATH);
      commandLine.addParameters(
        "refresh",
        "--intellij",
        "--intellijLauncher", "echo", // to avoid opening project again
        data.projectName
      );
      Process refresh = commandLine.createProcess();
      refresh.waitFor();
      return refresh.exitValue() == 0;
    }
    catch (Exception e) {
      LOG.warn(e);
      return false;
    }
  }

  private static Optional<FastpassData> extractFastpassData(Project project) {
    try {
      String path = project.getBasePath();
      if (path != null) {
        Path bloopSettingsPath = Paths.get(path, ".bloop", "bloop.settings.json");
        VirtualFile bloopSettings =
          LocalFileSystem.getInstance().findFileByIoFile(bloopSettingsPath.toFile());
        if (bloopSettings != null && bloopSettings.exists()) {
          String content = new String(bloopSettings.contentsToByteArray());
          Gson gson = new Gson();
          BloopSettings settings = gson.fromJson(content, BloopSettings.class);
          Pattern pattern = Pattern.compile("org\\.scalameta:metals_2\\.12:(.*)");
          Optional<String> version =
            settings.refreshProjectsCommand.stream().map(pattern::matcher).filter(Matcher::find).map(m -> m.group(1)).findFirst();
          return version.map(fastpassVersion -> {
            String projectName = settings.refreshProjectsCommand.get(settings.refreshProjectsCommand.size() - 1);
            return new FastpassData(fastpassVersion, projectName);
          });
        }
      }
    }
    catch (Exception e) {
      LOG.warn("Failed to extract fastpass data from " + project, e);
    }
    return Optional.empty();
  }

  @NotNull
  private static Stream<Project> allOpenBspProjects() {
    return Arrays.stream(ProjectManager.getInstance().getOpenProjects())
      .filter(PantsUtil::isBspProject);
  }

  private static Optional<String> systemVersion(String fastpass) {
    try {
      Process process = new ProcessBuilder(fastpass, "version").start();
      process.waitFor();
      String version = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8).trim();
      return Optional.of(version);
    }
    catch (Exception e) {
      LOG.warn("Failed to check fastpass version", e);
      return Optional.empty();
    }
  }
}
