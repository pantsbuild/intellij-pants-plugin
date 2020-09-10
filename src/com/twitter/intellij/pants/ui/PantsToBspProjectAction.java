// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.google.common.collect.Streams;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.bsp.FastpassUtils;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PantsToBspProjectAction extends AnAction implements DumbAware {
  private static final String BSP_LINKED_PROJECT_PATH = "bsp.linked_project_path";

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    boolean isPants = project != null && PantsUtil.isPantsProject(project);
    e.getPresentation().setEnabledAndVisible(isPants);
    if (isPants) {
      dependingOnBspProjectExistence(
        project,
        () -> e.getPresentation().setText("Create new BSP project based on this Pants project"),
        linkedBspProject -> e.getPresentation().setText("Open linked BSP project")
      );
    }
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      Messages.showInfoMessage("Project not found.", "Error");
      return;
    }
    dependingOnBspProjectExistence(
      project,
      () -> createBspProject(project),
      linkedBspProject -> ProjectUtil.openOrImport(Paths.get(linkedBspProject), new OpenProjectTask())
    );
  }

  private void createBspProject(Project project) {
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
            Path workspace = commandLine.getWorkDirectory().toPath();
            Optional<Path> projectPath = existingProjectPath(outputErr, workspace);

            projectPath.ifPresent(path -> registerNewBspProjectAndOpen(path, project));
            projectPath.orElseThrow(() -> new RuntimeException(formatError(output, outputErr)));
          } else {
            Path projectPath = newProjectPath(output);
            registerNewBspProjectAndOpen(projectPath, project);
          }
        }
        catch (Exception ex) {
          ApplicationManager.getApplication()
            .invokeLater(() -> Messages.showErrorDialog(project, ex.getMessage(), "Error"));
        }
      }
    });
  }

  private void registerNewBspProjectAndOpen(Path projectPath, Project project) {
    PropertiesComponent.getInstance(project).setValue(BSP_LINKED_PROJECT_PATH, projectPath.toString());
    ApplicationManager.getApplication()
      .invokeLater(() -> ProjectUtil.openOrImport(projectPath, new OpenProjectTask()));
  }

  private Optional<Path> existingProjectPath(String output, Path workspace) {
    Pattern pattern = Pattern.compile("can't create project named '(.*?)' because it already exists");
    Matcher matcher = pattern.matcher(output);
    if (matcher.find()) {
      String name = matcher.group(1);
      return Optional.of(workspace.resolveSibling("bsp-projects").resolve(name));
    } else {
      return Optional.empty();
    }
  }

  @NotNull
  private Path newProjectPath(String output) {
    int lastIndexOfNl = output.lastIndexOf("\n");
    String path = (lastIndexOfNl == -1) ? output : output.substring(lastIndexOfNl + 1);
    return Paths.get(path).toAbsolutePath();
  }

  private GeneralCommandLine createCommandLine(Project project) throws IOException {
    List<String> fastpassCommandBase = Arrays.asList(
      "create",
      "--intellij",
      "--no-bloop-exit",
      "--intellijLauncher", "echo"
    );
    List<String> targets = pantsTargets(project);
    List<String> fastpassCommand = Streams.concat(fastpassCommandBase.stream(), targets.stream()).collect(Collectors.toList());
    return FastpassUtils.makeFastpassCommand(project, fastpassCommand);
  }

  private List<String> pantsTargets(Project project) {
    String root = PantsUtil.findBuildRoot(project).map(VirtualFile::getPath).orElse("") + "/";
    PantsSettings pantsSettings = PantsSettings.getInstance(project);
    return pantsSettings.getLinkedProjectsSettings()
      .stream()
      .flatMap(projectSettings -> projectSettings.getSelectedTargetSpecs().stream())
      .map(target -> stripPrefix(target, root))
      .collect(Collectors.toList());
  }

  private String stripPrefix(String s, String prefix) {
    return s.startsWith(prefix) ? s.substring(prefix.length()) : s;
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

  private void dependingOnBspProjectExistence(Project project, Runnable onNoBspProject, Consumer<String> onBspProject) {
    PropertiesComponent properties = PropertiesComponent.getInstance(project);

    String linkedBspProject = properties.getValue(BSP_LINKED_PROJECT_PATH);
    if (linkedBspProject == null) {
      onNoBspProject.run();
    }
    else {
      if (LocalFileSystem.getInstance().findFileByPath(linkedBspProject) == null) {
        properties.unsetValue(BSP_LINKED_PROJECT_PATH);
        onNoBspProject.run();
      }
      else {
        onBspProject.accept(linkedBspProject);
      }
    }
  }
}
