// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.util.AtomicNotNullLazyValue;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.PantsExecutionException;
import com.twitter.intellij.pants.model.PantsCompileOptions;
import com.twitter.intellij.pants.model.PantsExecutionOptions;
import com.twitter.intellij.pants.service.project.model.TargetAddressInfo;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PantsCompileOptionsExecutor {
  protected static final Logger LOG = Logger.getInstance(PantsCompileOptionsExecutor.class);

  private final List<Process> myProcesses = ContainerUtil.createConcurrentList();

  private final PantsCompileOptions myOptions;
  private final File myWorkingDir;
  private final boolean myResolveJars;
  private final boolean myCompileWithIntellij;
  private final boolean myResolveSourcesForJars;
  private final List<String> myResolverExtensionClassNames;

  /**
   *  Add help-default goal to Pants to get all defaults in machine readable format
   */
  private final NotNullLazyValue<PantsCompilerOptions> compilerOptions = new AtomicNotNullLazyValue<PantsCompilerOptions>() {
    @NotNull
    @Override
    protected PantsCompilerOptions compute() {
      final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(getProjectPath());
      commandLine.addParameters("help-advanced", "compile", "--no-color");
      try {
        final ProcessOutput processOutput = getProcessOutput(commandLine, null);
        final boolean isIsolated = StringUtil.contains(processOutput.getStdout(), "default: isolated");
        final List<String> stdoutLines = processOutput.getStdoutLines(true);
        final boolean isZincForAll = StringUtil.contains(processOutput.getStdout(), "--compile-zinc-strategy");
        final boolean useJmakeForJava = StringUtil.contains(processOutput.getStdout(), "compile-java-use-jmake (default: True)");
        final int zincLineIndex = ContainerUtil.indexOf(
          stdoutLines,
          new Condition<String>() {
            @Override
            public boolean value(String line) {
              return StringUtil.contains(line, "compile-zinc-java-enabled");
            }
          }
        );
        final boolean zincForJava1 = zincLineIndex >= 0 &&
                                     StringUtil.contains(stdoutLines.get(zincLineIndex), "default: True");
        final boolean zincForJava2 = (zincLineIndex + 1) < stdoutLines.size() &&
                                     StringUtil.contains(stdoutLines.get(zincLineIndex + 1), "default: True");
        return new PantsCompilerOptions(isIsolated, zincForJava1 || zincForJava2, isZincForAll, useJmakeForJava);
      }
      catch (ExecutionException e) {
        LOG.warn(e);
        return new PantsCompilerOptions();
      }
    }
  };

  @NotNull
  public static PantsCompileOptionsExecutor create(
    @NotNull String externalProjectPath,
    @Nullable PantsExecutionSettings executionOptions,
    boolean resolveJars
  ) throws ExternalSystemException {
    PantsCompileOptions options;
    final int targetNameDelimiterIndex = externalProjectPath.indexOf(':');
    if (targetNameDelimiterIndex > 0) {
      // normalizing. we don't have per module settings and a linked project path of a module contains target name.
      final String buildFilePath = externalProjectPath.substring(0, targetNameDelimiterIndex);
      final String targetName = externalProjectPath.substring(targetNameDelimiterIndex + 1);
      options = new MyPantsCompileOptions(buildFilePath, new MyPantsExecutionOptions(Collections.singletonList(targetName)));
    } else if (executionOptions == null) {
      throw new ExternalSystemException("No execution options for " + externalProjectPath);
    } else {
      options = new MyPantsCompileOptions(externalProjectPath, executionOptions);
    }
    final File workingDir = PantsUtil.findPantsWorkingDir(new File(options.getExternalProjectPath()));
    if (workingDir == null || !workingDir.exists()) {
      throw new ExternalSystemException(PantsBundle.message("pants.error.no.pants.executable.by.path", options.getExternalProjectPath()));
    }
    return new PantsCompileOptionsExecutor(
      workingDir, options,
      resolveJars, executionOptions != null && executionOptions.isLibsWithSources(),
      executionOptions != null && executionOptions.isCompileWithIntellij(),
      executionOptions != null ? executionOptions.getResolverExtensionClassNames() : Collections.<String>emptyList()
    );
  }

  @NotNull
  @TestOnly
  public static PantsCompileOptionsExecutor createMock() {
    return new PantsCompileOptionsExecutor(
      new File(""),
      new MyPantsCompileOptions("", new PantsExecutionSettings()),
      false,
      true,
      false,
      Collections.<String>emptyList()
    ) {
      @Override
      public boolean isIsolatedStrategy() {
        return false;
      }
    };
  }

  private PantsCompileOptionsExecutor(
    @NotNull File workingDir,
    @NotNull PantsCompileOptions compilerOptions,
    boolean resolveJars,
    boolean resolveSourcesForJars,
    boolean compileWithIntellij,
    @NotNull List<String> resolverExtensionClassNames
  ) {
    myWorkingDir = workingDir;
    myOptions = compilerOptions;
    myResolveJars = resolveJars;
    myCompileWithIntellij = compileWithIntellij;
    myResolverExtensionClassNames = resolverExtensionClassNames;
    myResolveSourcesForJars = resolveSourcesForJars;
  }

  public String getProjectRelativePath() {
    return PantsUtil.getRelativeProjectPath(getWorkingDir(), getProjectPath());
  }

  @NotNull
  public File getWorkingDir() {
    return myWorkingDir;
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

  @NotNull @Nls
  public String getProjectName() {
    if (PantsUtil.isExecutable(myOptions.getExternalProjectPath())) {
      //noinspection ConstantConditions
      return PantsUtil.fileNameWithoutExtension(VfsUtil.extractFileName(myOptions.getExternalProjectPath()));
    }
    final String targetsSuffix = myOptions.getTargetNames().isEmpty() ? ":" : StringUtil.join(myOptions.getTargetNames(), " :");
    return getProjectRelativePath() + "/:" + targetsSuffix;
  }

  @NotNull
  public PantsCompileOptions getOptions() {
    return myOptions;
  }

  public boolean isCompileWithPants() {
    return !isCompileWithIntellij();
  }

  public boolean isCompileWithIntellij() {
    return myCompileWithIntellij;
  }

  @NotNull
  public List<String> getResolverExtensionClassNames() {
    return myResolverExtensionClassNames;
  }

  @NotNull
  public String loadProjectStructure(
    @NotNull Consumer<String> statusConsumer,
    @Nullable ProcessAdapter processAdapter
  ) throws IOException, ExecutionException {
    if (PantsUtil.isExecutable(getProjectPath())) {
      return loadProjectStructureFromScript(getProjectPath(), statusConsumer, processAdapter);
    } else {
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
    final ProcessOutput processOutput = getProcessOutput(command, processAdapter);
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
    // in unit test mode it's always preview but we need to know libraries
    // because some jvm_binary targets are actually Scala ones and we need to
    // set a proper com.twitter.intellij.pants.compiler output folder
    if (myResolveJars && myResolveSourcesForJars) {
      commandLine.addParameter("resolve.ivy");
      commandLine.addParameter("--confs=default");
      commandLine.addParameter("--confs=sources");
      commandLine.addParameter("--soft-excludes");
    }

    commandLine.addParameter("export");
    if (!myResolveJars) {
      commandLine.addParameter("--no-libraries");
    }
    commandLine.addParameters(getAllTargetAddresses());

    if (getOptions().isWithDependees()) {
      statusConsumer.consume( "Looking for dependents...");
      commandLine.addParameters(loadDependees(getAllTargetAddresses()));
    }

    commandLine.addParameter("--export-output-file=" + outputFile.getPath());
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
    if (!getOptions().getTargetNames().isEmpty()) {
      return ContainerUtil.map(
        getOptions().getTargetNames(),
        new Function<String, String>() {
          @Override
          public String fun(String targetName) {
            return getProjectRelativePath() + File.separator + ":" + targetName;
          }
        }
      );
    } else {
      return Collections.singletonList(getProjectRelativePath() + File.separator + "::");
    }
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

  public String compilerFolderForTarget(@NotNull TargetAddressInfo targetAddressInfo) {
    if (targetAddressInfo.isAnnotationProcessor()) {
      return "apt";
    }
    final PantsCompilerOptions options = compilerOptions.getValue();
    if (targetAddressInfo.isScala()) {
      return options.isZincForAll() ? "zinc" : "scala";
    }
    if (options.isZincForAll() && !options.isUseJmakeForJava()) {
      return "zinc";
    }
    return options.isCompileWithZincForJava() ? "zinc-java" : "java";
  }

  public boolean isIsolatedStrategy() {
    final boolean result = compilerOptions.getValue().isCompileWithIsolatedStrategy();
    if (!result && PantsUtil.isIsolatedStrategyTestFlagEnabled()) {
      throw new PantsException("Expected to use isolated strategy!");
    }
    return result;
  }

  public String getAbsolutePathFromWorkingDir(@NotNull String relativePath) {
    return new File(getWorkingDir(), relativePath).getPath();
  }

  private static class MyPantsCompileOptions implements PantsCompileOptions {

    private final String myExternalProjectPath;
    private final PantsExecutionOptions myExecutionOptions;

    public MyPantsCompileOptions(@NotNull String externalProjectPath, @NotNull PantsExecutionOptions executionOptions) {
      myExternalProjectPath = PantsUtil.resolveSymlinks(externalProjectPath);
      myExecutionOptions = executionOptions;
    }

    @NotNull
    @Override
    public String getExternalProjectPath() {
      return myExternalProjectPath;
    }

    @NotNull
    @Override
    public List<String> getTargetNames() {
      return myExecutionOptions.getTargetNames();
    }

    @Override
    public boolean isWithDependees() {
      return myExecutionOptions.isWithDependees();
    }
  }

  private static class MyPantsExecutionOptions implements PantsExecutionOptions {

    private final List<String> myTargetNames;

    public MyPantsExecutionOptions(@NotNull List<String> targetNames) {
      myTargetNames = targetNames;
    }

    @NotNull
    @Override
    public List<String> getTargetNames() {
      return myTargetNames;
    }

    @Override
    public boolean isWithDependees() {
      return false;
    }
  }

  private static class PantsCompilerOptions {
    private final boolean compileWithIsolatedStrategy;
    private final boolean compileWithZincForJava;
    private final boolean zincForAll;
    private final boolean useJmakeForJava;

    private PantsCompilerOptions() {
      this(false, false, false, false);
    }

    private PantsCompilerOptions(boolean compileWithIsolatedStrategy, boolean compileWithZincForJava, boolean zincForAll, boolean useJmakeForJava) {
      this.compileWithIsolatedStrategy = compileWithIsolatedStrategy;
      this.compileWithZincForJava = compileWithZincForJava;
      this.zincForAll = zincForAll;
      this.useJmakeForJava = useJmakeForJava;
    }

    public boolean isCompileWithIsolatedStrategy() {
      return compileWithIsolatedStrategy;
    }

    public boolean isCompileWithZincForJava() {
      return compileWithZincForJava;
    }

    public boolean isZincForAll() {
      return zincForAll;
    }

    public boolean isUseJmakeForJava() {
      return useJmakeForJava;
    }
  }
}
