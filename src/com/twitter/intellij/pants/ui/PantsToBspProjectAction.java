// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PantsToBspProjectAction extends AnAction implements DumbAware {
  private static final String METALS_VERSION = "0.8.4";

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    boolean isPants = project != null && PantsUtil.isPantsProject(project);
    e.getPresentation().setEnabledAndVisible(isPants);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      Messages.showInfoMessage("Project not found.", "Error");
      return;
    }

    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Preparing BSP Project", false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          GeneralCommandLine commandLine = createCommandLine(project);
          Process process = commandLine.createProcess();
          String output = readToString(process.getInputStream());
          String outputErr = readToString(process.getErrorStream());
          int result = process.waitFor();
          if (result != 0) {
            String message = formatError(output, outputErr);
            throw new RuntimeException(message);
          } else {
            Path projectPath = Paths.get(output);
            ProjectUtil.openOrImport(projectPath, new OpenProjectTask());
          }
        }
        catch (Exception ex) {
          ApplicationManager.getApplication()
            .invokeLater(() -> Messages.showErrorDialog(project, ex.getMessage(), "Error"));
        }
      }
    });
  }

  private GeneralCommandLine createCommandLine(Project project) throws IOException {
    GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(project);

    String coursier = coursierPath().toString();
    commandLine.setExePath(coursier);

    String name = project.getName();
    List<String> commandBase = Arrays.asList(
      "launch", "org.scalameta:metals_2.12:" + METALS_VERSION,
      "--main", "scala.meta.internal.pantsbuild.BloopPants",
      "--",
      "create",
      "--name", name,
      "--intellij",
      "--intellijLauncher", "echo"
    );
    commandLine.addParameters(commandBase);

    List<String> targets = pantsTargets(project);
    commandLine.addParameters(targets);
    return commandLine;
  }

  private Path coursierPath() throws IOException {
    Path destination = Paths.get(System.getProperty("java.io.tmpdir"), "pants-plugin-coursier");
    if (!Files.exists(destination)) {
      URL url = new URL("https://git.io/coursier-cli");
      Files.copy(url.openConnection().getInputStream(), destination);
      destination.toFile().setExecutable(true);
    }
    return destination;
  }

  private List<String> pantsTargets(Project project) {
    PantsSettings pantsSettings = PantsSettings.getInstance(project);
    return pantsSettings.getLinkedProjectsSettings()
      .stream()
      .flatMap(projectSettings -> projectSettings.getSelectedTargetSpecs().stream())
      .collect(Collectors.toList());
  }

  private String formatError(String output, String outputErr) {
    return "Converting pants project to bsp failed." + format("Standard output", output) + format("Error output", outputErr);
  }

  private String format(String title, String output) {
    return output.isEmpty() ? "" : "\n" + title + ":\n" + output;
  }

  private String readToString(InputStream stream) throws IOException {
    return IOUtils.toString(stream, StandardCharsets.UTF_8).trim();
  }

}
