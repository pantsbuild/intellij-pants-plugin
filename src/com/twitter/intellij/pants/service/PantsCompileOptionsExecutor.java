// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.Consumer;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.PantsExecutionException;
import com.twitter.intellij.pants.components.impl.PantsMetrics;
import com.twitter.intellij.pants.model.PantsCompileOptions;
import com.twitter.intellij.pants.model.PantsExecutionOptions;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class PantsCompileOptionsExecutor {
  protected static final Logger LOG = Logger.getInstance(PantsCompileOptionsExecutor.class);

  private final List<Process> myProcesses = ContainerUtil.createConcurrentList();

  private final PantsCompileOptions myOptions;
  private final File myBuildRoot;
  private final boolean myResolveSourcesAndDocsForJars;

  @NotNull
  public static PantsCompileOptionsExecutor create(
    @NotNull String projectRootPath,
    @Nullable PantsExecutionSettings executionOptions
  ) throws ExternalSystemException {
    if (executionOptions == null) {
      throw new ExternalSystemException("No execution options for " + projectRootPath);
    }
    PantsCompileOptions options = new MyPantsCompileOptions(projectRootPath, executionOptions);

    Optional<File> buildRoot = PantsUtil.findBuildRoot(new File(options.getExternalProjectPath()));
    if (!buildRoot.isPresent() || !buildRoot.get().exists()) {
      throw new ExternalSystemException(PantsBundle.message("pants.error.no.pants.executable.by.path", options.getExternalProjectPath()));
    }
    return new PantsCompileOptionsExecutor(
      buildRoot.get(),
      options,
      executionOptions.isLibsWithSourcesAndDocs()
    );
  }

  @NotNull
  @TestOnly
  public static PantsCompileOptionsExecutor createMock() {
    return new PantsCompileOptionsExecutor(
      new File("/"),
      new MyPantsCompileOptions("", PantsExecutionSettings.createDefault()),
      true
    ) {
    };
  }

  private PantsCompileOptionsExecutor(
    @NotNull File buildRoot,
    @NotNull PantsCompileOptions compilerOptions,
    boolean resolveSourcesAndDocsForJars
  ) {
    myBuildRoot = buildRoot;
    myOptions = compilerOptions;
    myResolveSourcesAndDocsForJars = resolveSourcesAndDocsForJars;
  }

  public String getProjectRelativePath() {
    return PantsUtil.getRelativeProjectPath(getBuildRoot(), getProjectPath()).get();
  }

  @NotNull
  public File getBuildRoot() {
    return myBuildRoot;
  }

  public String getProjectPath() {
    return myOptions.getExternalProjectPath();
  }

  @NotNull
  public String getProjectDir() {
    final File projectFile = new File(getProjectPath());
    final File projectDir = projectFile.isDirectory() ? projectFile : FileUtil.getParentFile(projectFile);
    return projectDir != null ? projectDir.getAbsolutePath() : projectFile.getAbsolutePath();
  }

  @NotNull
  @Nls
  public String getProjectName() {
    return String.join("__", myOptions.getTargetSpecs());
  }

  @NotNull
  @Nls
  public String getRootModuleName() {
    if (PantsUtil.isExecutable(myOptions.getExternalProjectPath())) {
      //noinspection ConstantConditions
      return PantsUtil.fileNameWithoutExtension(VfsUtil.extractFileName(myOptions.getExternalProjectPath()));
    }
    return getProjectRelativePath();
  }

  @NotNull
  public PantsCompileOptions getOptions() {
    return myOptions;
  }

  @NotNull
  public String loadProjectStructure(
    @NotNull Consumer<String> statusConsumer,
    @Nullable ProcessAdapter processAdapter
  ) throws IOException, ExecutionException {
    if (PantsUtil.isExecutable(getProjectPath())) {
      return loadProjectStructureFromScript(getProjectPath(), statusConsumer, processAdapter);
    }
    else {
      return loadProjectStructureFromTargets(statusConsumer, processAdapter);
    }
  }

  @NotNull
  private static String loadProjectStructureFromScript(
    @NotNull String scriptPath,
    @NotNull Consumer<String> statusConsumer,
    @Nullable ProcessAdapter processAdapter
  ) throws IOException, ExecutionException {
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(scriptPath);
    commandLine.setExePath(scriptPath);
    statusConsumer.consume("Executing " + PathUtil.getFileName(scriptPath));
    final ProcessOutput processOutput = PantsUtil.getCmdOutput(commandLine, processAdapter);
    if (processOutput.checkSuccess(LOG)) {
      return processOutput.getStdout();
    }
    else {
      throw new PantsExecutionException("Failed to update the project!", scriptPath, processOutput);
    }
  }

  @NotNull
  private String loadProjectStructureFromTargets(
    @NotNull Consumer<String> statusConsumer,
    @Nullable ProcessAdapter processAdapter
  ) throws IOException, ExecutionException {
    final File outputFile = FileUtil.createTempFile("pants_depmap_run", ".out");
    final GeneralCommandLine command = getCommand(outputFile, statusConsumer);
    statusConsumer.consume("Resolving dependencies...");
    PantsMetrics.markExportStart();
    final ProcessOutput processOutput = getProcessOutput(command, processAdapter);
    PantsMetrics.markExportEnd();
    if (processOutput.getStdout().contains("no such option")) {
      throw new ExternalSystemException("Pants doesn't have necessary APIs. Please upgrade you pants!");
    }
    if (processOutput.checkSuccess(LOG)) {
      return FileUtil.loadFile(outputFile);
    }
    else {
      throw new PantsExecutionException("Failed to update the project!", command.getCommandLineString("pants"), processOutput);
    }
  }

  private ProcessOutput getProcessOutput(
    @NotNull GeneralCommandLine command,
    @Nullable ProcessAdapter processAdapter
  ) throws ExecutionException {
    final Process process = command.createProcess();
    myProcesses.add(process);
    final ProcessOutput processOutput = PantsUtil.getOutput(process, processAdapter);
    myProcesses.remove(process);
    return processOutput;
  }

  @NotNull
  private GeneralCommandLine getCommand(final File outputFile, @NotNull Consumer<String> statusConsumer)
    throws IOException, ExecutionException {
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(getProjectPath());
    commandLine.addParameter("export");
    if (myResolveSourcesAndDocsForJars) {
      commandLine.addParameter("--export-libraries-sources");
      commandLine.addParameter("--export-libraries-javadocs");
    }

    commandLine.addParameters(getAllTargetAddresses());
    System.out.println(getAllTargetAddresses());
    if (getOptions().isWithDependees()) {
      statusConsumer.consume("Looking for dependents...");
      commandLine.addParameters(loadDependees(getAllTargetAddresses()));
    }

    commandLine.addParameter("--export-output-file=" + outputFile.getPath());
    LOG.debug(commandLine.toString());
    return commandLine;
  }

  private List<String> loadDependees(List<String> addresses) throws IOException, ExecutionException {
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(getProjectPath());
    commandLine.addParameter("dependees");
    commandLine.addParameter("--transitive");
    commandLine.addParameters(addresses);

    final File outputFile = FileUtil.createTempFile("pants_depmap_run", ".out");
    commandLine.addParameter("--dependees-output-file=" + outputFile.getPath());

    final ProcessOutput output = getProcessOutput(commandLine, null);
    if (!output.checkSuccess(LOG)) {
      throw new ExternalSystemException("Failed to find dependents!\n" + output.getStderr());
    }

    return FileUtil.loadLines(outputFile);
  }

  @NotNull
  private List<String> getAllTargetAddresses() {
    // If project is opened via pants cli, the targets are in specs.
    return getOptions().getTargetSpecs();
  }

  /**
   * @return if successfully canceled all running processes. false if failed ot there were no processes to cancel.
   */
  public boolean cancelAllProcesses() {
    if (myProcesses.isEmpty()) {
      return false;
    }
    for (Process process : myProcesses) {
      process.destroy();
    }
    return true;
  }

  public String getAbsolutePathFromWorkingDir(@NotNull String relativePath) {
    return new File(getBuildRoot(), relativePath).getPath();
  }

  private static class MyPantsCompileOptions implements PantsCompileOptions {

    private final String myExternalProjectPath;
    private final PantsExecutionOptions myExecutionOptions;

    private MyPantsCompileOptions(@NotNull String externalProjectPath, @NotNull PantsExecutionOptions executionOptions) {
      myExternalProjectPath = PantsUtil.resolveSymlinks(externalProjectPath);
      myExecutionOptions = executionOptions;
    }

    @NotNull
    @Override
    public String getExternalProjectPath() {
      return myExternalProjectPath;
    }

    @NotNull
    public List<String> getTargetSpecs() {
      return myExecutionOptions.getTargetSpecs();
    }

    @Override
    public boolean isWithDependees() {
      return myExecutionOptions.isWithDependees();
    }

    public boolean isEnableIncrementalImport() {
      return myExecutionOptions.isEnableIncrementalImport();
    }
  }
}
