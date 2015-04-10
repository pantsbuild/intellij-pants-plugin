// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.google.gson.JsonSyntaxException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsExecutionException;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsScalaUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public abstract class PantsResolverBase {
  protected static final Logger LOG = Logger.getInstance(PantsResolver.class);
  protected final boolean myGenerateJars;
  protected final String myProjectPath;
  protected final PantsExecutionSettings mySettings;
  protected File myWorkDirectory = null;
  protected ProjectInfo myProjectInfo = null;

  public PantsResolverBase(
    @NotNull String projectPath,
    @NotNull PantsExecutionSettings settings,
    boolean isPreviewMode
  ) {
    myProjectPath = projectPath;
    mySettings = settings;
    myGenerateJars = !isPreviewMode;
    myWorkDirectory = PantsUtil.findPantsWorkingDir(new File(projectPath));
  }

  public static ProjectInfo parseProjectInfoFromJSON(String data) throws JsonSyntaxException {
    return ProjectInfo.fromJson(data);
  }

  @Nullable
  public ProjectInfo getProjectInfo() {
    return myProjectInfo;
  }

  @TestOnly
  protected void setWorkDirectory(@Nullable File workDirectory) {
    myWorkDirectory = workDirectory;
  }

  @TestOnly
  public void setProjectInfo(ProjectInfo projectInfo) {
    myProjectInfo = projectInfo;
  }

  private void parse(final String output) {
    myProjectInfo = null;
    if (output.isEmpty()) throw new ExternalSystemException("Not output from pants");
    try {
      myProjectInfo = parseProjectInfoFromJSON(output);
    }
    catch (JsonSyntaxException e) {
      LOG.warn("Can't parse output\n" + output, e);
      throw new ExternalSystemException("Can't parse project structure!");
    }
  }

  abstract void addInfoTo(@NotNull DataNode<ProjectData> projectInfoDataNode);

  public void resolve(Consumer<String> statusConsumer, @Nullable ProcessAdapter processAdapter) {
    try {
      final File outputFile = FileUtil.createTempFile("pants_depmap_run", ".out");
      List<String> additionalTargets = Collections.emptyList();
      if (mySettings.isWithDependees()) {
        statusConsumer.consume( "Looking for dependees...");
        additionalTargets = loadDependees(getAllTargetAddresses());
      }
      final GeneralCommandLine command = getCommand(outputFile, additionalTargets);
      statusConsumer.consume( "Resolving dependencies...");
      final ProcessOutput processOutput = PantsUtil.getCmdOutput(command, processAdapter);
      if (processOutput.getStdout().contains("no such option")) {
        throw new ExternalSystemException("Pants doesn't have necessary APIs. Please upgrade you pants!");
      }
      if (processOutput.checkSuccess(LOG)) {
        final String output = FileUtil.loadFile(outputFile);
        parse(output);

        final File bootstrapBuildFile = new File(command.getWorkDirectory(), "BUILD");
        if (bootstrapBuildFile.exists() && myProjectInfo != null && PantsScalaUtil.hasMissingScalaCompilerLibs(myProjectInfo)) {
          // need to bootstrap tools
          statusConsumer.consume("Bootstrapping tools...");
          final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(bootstrapBuildFile.getPath());
          commandLine.addParameters("resolve", "BUILD:");
          PantsUtil.getCmdOutput(commandLine, null).checkSuccess(LOG);
        }
      }
      else {
        throw new PantsExecutionException("Failed to update the project!", command.getCommandLineString("pants"), processOutput);
      }
    }
    catch (ExecutionException e) {
      throw new ExternalSystemException(e);
    }
    catch (IOException ioException) {
      throw new ExternalSystemException(ioException);
    }
  }

  private List<String> loadDependees(List<String> addresses) throws IOException, ExecutionException {
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(myProjectPath);
    commandLine.addParameter("dependees");
    commandLine.addParameters(addresses);

    final File outputFile = FileUtil.createTempFile("pants_depmap_run", ".out");
    commandLine.addParameter("--dependees-output-file=" + outputFile.getPath());

    if (!PantsUtil.getCmdOutput(commandLine, null).checkSuccess(LOG)) {
      throw new ExternalSystemException("Failed to find dependees!");
    }

    return FileUtil.loadLines(outputFile);
  }

  protected GeneralCommandLine getCommand(final File outputFile, List<String> additionalTargets) {
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(myProjectPath);
    // in unit test mode it's always preview but we need to know libraries
    // because some jvm_binary targets are actually Scala ones and we need to
    // set a proper com.twitter.intellij.pants.compiler output folder
    if (myGenerateJars || ApplicationManager.getApplication().isUnitTestMode()) {
      commandLine.addParameter("resolve.ivy");
      commandLine.addParameter("--confs=default");
      commandLine.addParameter("--confs=sources");
      commandLine.addParameter("--soft-excludes");
    }

    commandLine.addParameter("export");
    commandLine.addParameters(getAllTargetAddresses());
    commandLine.addParameters(additionalTargets);

    commandLine.addParameter("--thrift-linter-skip");
    commandLine.addParameter("--export-output-file=" + outputFile.getPath());
    return commandLine;
  }

  private List<String> getAllTargetAddresses() {
    final String relativeProjectPath = PantsUtil.getRelativeProjectPath(myWorkDirectory, myProjectPath);

    if (relativeProjectPath == null) {
      throw new ExternalSystemException(
        String.format(
          "Can't find relative path for a target %s from dir %s",
          myProjectPath, myWorkDirectory != null ? myWorkDirectory.getPath() : null
        )
      );
    }

    if (mySettings.isAllTargets()) {
      return Collections.singletonList(relativeProjectPath + File.separator + "::");
    }
    else {
      return ContainerUtil.map(
        mySettings.getTargetNames(),
        new Function<String, String>() {
          @Override
          public String fun(String targetName) {
            return relativeProjectPath + File.separator + ":" + targetName;
          }
        }
      );
    }
  }
}
