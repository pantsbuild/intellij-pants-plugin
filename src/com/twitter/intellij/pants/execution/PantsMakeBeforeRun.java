// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.CommonProgramRunConfigurationParameters;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.configurations.WrappingRunConfiguration;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.process.CapturingAnsiEscapesAwareProcessHandler;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.file.FileChangeTracker;
import com.twitter.intellij.pants.metrics.PantsExternalMetricsListenerManager;
import com.twitter.intellij.pants.model.IJRC;
import com.twitter.intellij.pants.model.PantsOptions;
import com.twitter.intellij.pants.service.project.FastpassRecommendationNotificationService;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.ui.PantsConsoleManager;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;

import javax.swing.Icon;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * PantsMakeBeforeRun creates a custom Make process `PantsCompile` to replace IntelliJ's default Make process whenever a new configuration
 * is added under a pants project via {@link PantsMakeBeforeRun#replaceDefaultMakeWithPantsMake}, so the time to launch pants is minimized.
 * <p/>
 * Motivation: By default, IntelliJ's Make process is invoked before any JUnit/Scala/Application run which has unnecessary (for Pants)
 * long steps to scan the entire project to assist external builders' incremental compile.
 */
public class PantsMakeBeforeRun extends ExternalSystemBeforeRunTaskProvider {

  public static final Key<ExternalSystemBeforeRunTask> ID = Key.create("Pants.BeforeRunTask");
  public static final String ERROR_TAG = "[error]";
  private static final ConcurrentHashMap<Project, Process> runningPantsProcesses = new ConcurrentHashMap<>();

  public PantsMakeBeforeRun(@NotNull Project project) {
    super(PantsConstants.SYSTEM_ID, project, ID);
  }

  public static boolean hasActivePantsProcess(@NotNull Project project) {
    return runningPantsProcesses.containsKey(project);
  }

  public static void setRunConfigurationWorkingDirectory(@NotNull RunConfiguration runConfiguration) {
    Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(runConfiguration.getProject());

    /**
     /**
     * Scala related run/test configuration inherit {@link AbstractTestRunConfiguration}
     * Use string test on class name due to scala plugin can be optional and it is hard to separate this logic.
     */
    if (PantsUtil.isScalaRelatedTestRunConfiguration(runConfiguration)) {
      buildRoot
        .map(VirtualFile::getPath)
        .ifPresent(((AbstractTestRunConfiguration) runConfiguration).testConfigurationData()::setWorkingDirectory);
    }
    /**
     * JUnit, Application, etc configuration inherit {@link CommonProgramRunConfigurationParameters}
     */
    else if (runConfiguration instanceof CommonProgramRunConfigurationParameters) {
      buildRoot
        .map(VirtualFile::getPath)
        .ifPresent(((CommonProgramRunConfigurationParameters) runConfiguration)::setWorkingDirectory);
    }
  }

  public static void replaceDefaultMakeWithPantsMake(@NotNull RunConfiguration runConfiguration) {
    if (!PantsUtil.isScalaRelatedTestRunConfiguration(runConfiguration) &&
        !(runConfiguration instanceof CommonProgramRunConfigurationParameters)) {
      return;
    }

    RunManager runManager = RunManager.getInstance(runConfiguration.getProject());
    RunManagerImpl runManagerImpl = (RunManagerImpl) runManager;
    BeforeRunTask pantsMakeTask = new ExternalSystemBeforeRunTask(ID, PantsConstants.SYSTEM_ID);
    pantsMakeTask.setEnabled(true);
    runManagerImpl.setBeforeRunTasks(runConfiguration, Collections.singletonList(pantsMakeTask));
  }

  public static void terminatePantsProcess(Project project) {
    Process process = runningPantsProcesses.get(project);
    if (process != null) {
      process.destroy();
      runningPantsProcesses.remove(project, process);
    }
  }

  @Override
  public String getName() {
    return "PantsCompile";
  }

  @Override
  public String getDescription(ExternalSystemBeforeRunTask task) {
    return "PantsCompile";
  }

  @NotNull
  @Override
  public ExternalSystemBeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
    return new ExternalSystemBeforeRunTask(ID, PantsConstants.SYSTEM_ID);
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return PantsIcons.Icon;
  }

  @Override
  public boolean canExecuteTask(@NotNull RunConfiguration configuration, @NotNull ExternalSystemBeforeRunTask beforeRunTask) {
    return true;
  }

  @Override
  public boolean executeTask(
    @NotNull DataContext context,
    RunConfiguration configuration,
    @NotNull ExecutionEnvironment env,
    @NotNull ExternalSystemBeforeRunTask beforeRunTask
  ) {
    Project currentProject = configuration.getProject();
    Set<String> targetAddressesToCompile = PantsUtil.filterGenTargets(getTargetAddressesToCompile(configuration));
    Stopwatch sw = Stopwatch.createStarted();
    PantsExecuteTaskResult result = executeCompileTask(currentProject, targetAddressesToCompile, false);
    Duration buildDuration = sw.elapsed();
    FastpassRecommendationNotificationService.getInstance().tick(configuration.getProject(), buildDuration);
    return result.succeeded;
  }

  public PantsExecuteTaskResult doPantsCompile(@NotNull Project project) {
    return executeCompileTask(project, getTargetAddressesToCompile(ModuleManager.getInstance(project).getModules()), false);
  }

  public PantsExecuteTaskResult doPantsCompile(@NotNull Module[] modules) {
    if (modules.length == 0) {
      return PantsExecuteTaskResult.emptyFailure();
    }
    return executeCompileTask(modules[0].getProject(), getTargetAddressesToCompile(modules), false);
  }

  /**
   * Execute a list of tasks on a set of target addresses, doing some compile-specific checks.
   *
   * @param currentProject:  current project
   * @param targetAddresses: set of target addresses given to pants executable (e.g. "::")
   * @param tasks:           set of tasks given to pants executable (e.g. "lint")
   * @param opTitle:         simple title describing what this invocation is doing (e.g. "Compile")
   * @return whether the execution is successful and an optional message
   * describing the result as a PantsExecuteTaskResult object
   */
  public PantsExecuteTaskResult invokePants(
    @NotNull Project currentProject,
    @NotNull Set<String> targetAddresses,
    @NotNull List<String> tasks,
    @NotNull String opTitle
  ) {
    prepareIDE(currentProject);

    if (targetAddresses.isEmpty()) {
      showPantsMakeTaskMessage("No target found in configuration.\n", ConsoleViewContentType.SYSTEM_OUTPUT, currentProject);
      return new PantsExecuteTaskResult(true, Optional.empty());
    }
    if (tasks.isEmpty()) {
      showPantsMakeTaskMessage("No tasks specified in Pants invocation.\n", ConsoleViewContentType.ERROR_OUTPUT, currentProject);
      return PantsExecuteTaskResult.emptyFailure();
    }

    Optional<VirtualFile> pantsExecutable = PantsUtil.findPantsExecutable(currentProject);
    if (!pantsExecutable.isPresent()) {
      return new PantsExecuteTaskResult(
        false,
        Optional.of(
          PantsBundle.message("pants.error.no.pants.executable.by.path", currentProject.getProjectFilePath())
        )
      );
    }
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(pantsExecutable.get().getPath());

    showPantsMakeTaskMessage("Checking Pants options...\n", ConsoleViewContentType.SYSTEM_OUTPUT, currentProject);
    Optional<PantsOptions> projectOptsResult = PantsOptions.getPantsOptions(currentProject);
    if (!projectOptsResult.isPresent()) {
      showPantsMakeTaskMessage("Pants Options not found.\n", ConsoleViewContentType.ERROR_OUTPUT, currentProject);
      return PantsExecuteTaskResult.emptyFailure();
    }
    PantsOptions pantsOptions = projectOptsResult.get();

    /* Global options section. */
    commandLine.addParameter(PantsConstants.PANTS_CLI_OPTION_NO_COLORS);

    if (tasks.contains(PantsConstants.PANTS_TASK_CLEAN_ALL) && pantsOptions.supportsAsyncCleanAll()) {
      commandLine.addParameter(PantsConstants.PANTS_CLI_OPTION_ASYNC_CLEAN_ALL);
    }
    if (tasks.contains(PantsConstants.PANTS_TASK_EXPORT_CLASSPATH)) {
      commandLine.addParameter(PantsConstants.PANTS_CLI_OPTION_EXPORT_CLASSPATH_MANIFEST_JAR);
    }

    PantsSettings settings = PantsSettings.getInstance(currentProject);
    try {
      String javaHome;
      if (settings.isUseIdeaProjectJdk()) {
        javaHome = PantsUtil.getJdkPathFromIntelliJCore();
      }
      else {
        Sdk sdk = ProjectRootManager.getInstance(currentProject).getProjectSdk();
        javaHome = sdk.getHomeDirectory().getPath();
      }

      commandLine.addParameter(PantsUtil.getJvmDistributionPathParameter(javaHome));
    }
    catch (Exception e) {
      showPantsMakeTaskMessage(e.getMessage() != null ? e.getMessage() : e.toString(), ConsoleViewContentType.ERROR_OUTPUT, currentProject);
      return PantsExecuteTaskResult.emptyFailure();
    }

    /* Add `.ic.iterate.rc` file */
    final Optional<String> rcIterate = IJRC.getIteratePantsRc(commandLine.getWorkDirectory().getPath());
    rcIterate.ifPresent(commandLine::addParameter);

    /* Goals and targets section. */
    commandLine.addParameters(tasks);
    commandLine.addParameters(Lists.newArrayList(targetAddresses));

    /* Invoke the Pants subprocess. */
    final Process process;
    try {
      process = commandLine.createProcess();
    }
    catch (ExecutionException e) {
      showPantsMakeTaskMessage(e.getMessage() != null ? e.getMessage() : e.toString(), ConsoleViewContentType.ERROR_OUTPUT, currentProject);
      return PantsExecuteTaskResult.emptyFailure();
    }

    final CapturingProcessHandler processHandler = new CapturingAnsiEscapesAwareProcessHandler(process, commandLine.getCommandLineString());
    final List<String> output = new ArrayList<>();
    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
        super.onTextAvailable(event, outputType);
        showPantsMakeTaskMessage(event.getText(), ConsoleViewContentType.NORMAL_OUTPUT, currentProject);
        output.add(event.getText());
      }
    });
    runningPantsProcesses.put(currentProject, process);
    processHandler.runProcess();
    runningPantsProcesses.remove(currentProject, process);

    final boolean success = process.exitValue() == 0;
    if (tasks.contains(PantsConstants.PANTS_TASK_COMPILE)) {
      if (success) {
        // manifest jar is always created if the run succeeds
        FileChangeTracker.addManifestJarIntoSnapshot(currentProject);
      }
      else {
        // Mark project dirty if compile failed.
        FileChangeTracker.markDirty(currentProject);
      }

      // Sync files as generated sources may have changed after Pants compile.
      PantsUtil.synchronizeFiles();
    }

    String notifTitle = String.format("%s message", opTitle);
    String resultDescription = String.format("Pants %s %s", opTitle, success ? "succeeded" : "failed");
    NotificationType notifType = success ? NotificationType.INFORMATION : NotificationType.ERROR;
    notify(notifTitle, resultDescription, notifType);

    String finalOutString = String.join("", output);
    return new PantsExecuteTaskResult(success, Optional.of(finalOutString));
  }

  /**
   * @param currentProject:           current project
   * @param targetAddressesToCompile: set of target addresses given to pants executable (e.g. "::")
   * @param useCleanAll:              whether to run "clean-all" before the compilation
   * @return the result of invoking pants with the "compile" task on the given targets.
   */
  public PantsExecuteTaskResult executeCompileTask(
    @NotNull Project currentProject,
    @NotNull Set<String> targetAddressesToCompile,
    boolean useCleanAll
  ) {

    ApplicationManager.getApplication().invokeAndWait(() -> {
      /* Force cached changes to disk so {@link com.twitter.intellij.pants.file.FileChangeTracker} can mark the project dirty. */
      FileDocumentManager.getInstance().saveAllDocuments();
      currentProject.save();
    }, ModalityState.NON_MODAL);

    // If project has not changed since last Compile, return immediately.
    if (!FileChangeTracker.shouldRecompileThenReset(currentProject, targetAddressesToCompile)) {
      PantsExternalMetricsListenerManager.getInstance().logIsPantsNoopCompile(true);
      notify("Compile message", "Already up to date.", NotificationType.INFORMATION);
      return new PantsExecuteTaskResult(true, Optional.of(PantsConstants.NOOP_COMPILE));
    }
    List<String> compileTasks = Lists.newArrayList();
    if (useCleanAll) {
      compileTasks.add("clean-all");
    }
    compileTasks.addAll(Lists.newArrayList("export-classpath", "compile"));
    return invokePants(currentProject, targetAddressesToCompile, compileTasks, "Compile");
  }

  private void notify(final String title, final String subtitle, NotificationType type) {
    /* Show pop up notification about pants compile result. */
    ApplicationManager.getApplication().invokeLater(() -> {
      Notification start = new Notification(PantsConstants.PANTS, PantsIcons.Icon, title, subtitle, null, type, null);
      Notifications.Bus.notify(start);
    });
  }

  private void prepareIDE(Project project) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      /* Clear message window. */
      ConsoleView executionConsole = PantsConsoleManager.getConsole(project);
      executionConsole.getComponent().setVisible(true);
      executionConsole.clear();
      ToolWindowManagerEx.getInstance(project).getToolWindow(PantsConstants.PANTS_CONSOLE_NAME).activate(null);
    }, ModalityState.NON_MODAL);
  }

  @NotNull
  protected Set<String> getTargetAddressesToCompile(RunConfiguration configuration) {
    /* Scala run configurations */
    if (PantsUtil.isScalaRelatedTestRunConfiguration(configuration)) {
      RunConfiguration conf;
      if(configuration instanceof WrappingRunConfiguration) {
        WrappingRunConfiguration<RunConfigurationBase> wrapper = (WrappingRunConfiguration<RunConfigurationBase>) configuration;
        RunConfigurationBase base = wrapper.getPeer();
        conf = base.clone();
      } else {
        conf = configuration;
      }
      Module module = ((ModuleBasedConfiguration) conf).getConfigurationModule().getModule();
      return getTargetAddressesToCompile(new Module[]{module});
    }
    /* JUnit, Application run configurations */
    else if (configuration instanceof RunProfileWithCompileBeforeLaunchOption) {
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


  private void showPantsMakeTaskMessage(String message, ConsoleViewContentType type, Project project) {
    ConsoleView executionConsole = PantsConsoleManager.getConsole(project);
    // Create a filter that monitors console outputs, and turns them into a hyperlink if applicable.
    Filter filter = (line, entireLength) -> {
      Optional<ParseResult> result = ParseResult.parseErrorLocation(line, ERROR_TAG);
      if (result.isPresent()) {

        OpenFileHyperlinkInfo linkInfo = new OpenFileHyperlinkInfo(
          project,
          result.get().getFile(),
          result.get().getLineNumber() - 1, // line number needs to be 0 indexed
          result.get().getColumnNumber() - 1 // column number needs to be 0 indexed
        );
        int startHyperlink = entireLength - line.length() + line.indexOf(ERROR_TAG);

        return new Filter.Result(
          startHyperlink,
          entireLength,
          linkInfo,
          null // TextAttributes, going with default hence null
        );
      }
      return null;
    };

    ApplicationManager.getApplication().invokeLater(() -> {
      executionConsole.addMessageFilter(filter);
      executionConsole.print(message, type);
    }, ModalityState.NON_MODAL);
  }

  /**
   * Encapsulate the result of parsed data.
   */
  static class ParseResult {
    private final VirtualFile file;
    private final int lineNumber;
    private final int columnNumber;


    private ParseResult(VirtualFile file, int lineNumber, int columnNumber) {
      this.file = file;
      this.lineNumber = lineNumber;
      this.columnNumber = columnNumber;
    }

    /**
     * This function parses Pants output against known file and tag,
     * and returns (file, line number, column number)
     * encapsulated in `ParseResult` object if the output contains valid information.
     *
     * @param line original Pants output
     * @param tag  known tag. e.g. [error]
     * @return `ParseResult` instance
     */
    public static Optional<ParseResult> parseErrorLocation(String line, String tag) {
      if (!line.contains(tag)) {
        return Optional.empty();
      }

      String[] splitByColon = line.split(":");
      if (splitByColon.length < 3) {
        return Optional.empty();
      }

      try {
        // filePath path is between tag and first colon
        String filePath = splitByColon[0].substring(splitByColon[0].indexOf(tag) + tag.length()).trim();
        VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (virtualFile == null) {
          return Optional.empty();
        }
        // line number is between first and second colon
        int lineNumber = Integer.parseInt(splitByColon[1]);
        // column number is between second and third colon
        int columnNumber = Integer.parseInt(splitByColon[2]);
        return Optional.of(new ParseResult(virtualFile, lineNumber, columnNumber));
      }
      catch (NumberFormatException e) {
        return Optional.empty();
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ParseResult other = (ParseResult) obj;
      return Objects.equals(file, other.file)
             && Objects.equals(lineNumber, other.lineNumber)
             && Objects.equals(columnNumber, other.columnNumber);
    }

    public VirtualFile getFile() {
      return file;
    }

    public int getLineNumber() {
      return lineNumber;
    }

    public int getColumnNumber() {
      return columnNumber;
    }
  }
}
