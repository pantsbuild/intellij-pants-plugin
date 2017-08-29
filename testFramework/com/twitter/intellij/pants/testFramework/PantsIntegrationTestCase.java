// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.util.gotoByName.GotoFileModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.test.ExternalSystemImportingTestCase;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.CompilerTester;
import com.intellij.testFramework.ThreadTracker;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.metrics.PantsMetrics;
import com.twitter.intellij.pants.model.PantsOptions;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestConfigurationType;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * If your integration test modifies any source files
 * please set {@link PantsIntegrationTestCase#readOnly} to false.
 *
 * @see com.twitter.intellij.pants.highlighting.PantsHighlightingIntegrationTest
 */
public abstract class PantsIntegrationTestCase extends ExternalSystemImportingTestCase {

  private final boolean readOnly;
  private PantsProjectSettings myProjectSettings;
  private String myRelativeProjectPath = null;
  private CompilerTester myCompilerTester;

  protected PantsIntegrationTestCase() {
    this(true);
  }

  protected PantsIntegrationTestCase(boolean readOnly) {
    this.readOnly = readOnly;
  }

  @Override
  public void setUp() throws Exception {
    cleanProjectIdeaDir();
    super.setUp();
    VfsRootAccess.allowRootAccess("/");
    for (String pluginId : getRequiredPluginIds()) {
      final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(pluginId));
      assertNotNull(pluginId + " plugin should be in classpath for integration tests!", plugin);
      assertTrue(pluginId + " is not enabled!", plugin.isEnabled());
    }

    myProjectSettings = new PantsProjectSettings();
    myCompilerTester = null;
  }

  protected String[] getRequiredPluginIds() {
    return new String[]{
      "org.intellij.scala",
      "org.jetbrains.plugins.gradle",
      PantsConstants.PLUGIN_ID
    };
  }

  @Override
  protected void setUpInWriteAction() throws Exception {
    super.setUpInWriteAction();

    cleanProjectRoot();

    myProjectRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(getProjectFolder());
    assertNotNull(myProjectRoot);

    final File projectDir = new File(myProjectRoot.getPath());
    for (File projectTemplateFolder : getProjectFoldersToCopy()) {
      if (!projectTemplateFolder.exists() || !projectTemplateFolder.isDirectory()) {
        fail("invalid template project path " + projectTemplateFolder.getAbsolutePath());
      }

      PantsUtil.copyDirContent(projectTemplateFolder, projectDir);
    }
  }

  protected void passthroughSetUpInWriteAction() throws Exception {
    super.setUpInWriteAction();
  }

  protected void cleanProjectIdeaDir() throws ExecutionException, IOException {
    final File projectDir = new File(getProjectFolder().getPath());
    assertTrue("Failed to clean up!", FileUtil.delete(new File(projectDir, ".idea")));
  }

  protected void cleanProjectRoot() throws ExecutionException, IOException {
    final File projectDir = new File(myProjectRoot.getPath());
    assertTrue(projectDir.exists());
    if (readOnly) {
      final File originalIni = new File(projectDir, "pants.ini");
      final File originalIniCopy = new File(projectDir, "pants.ini.copy");
      if (originalIniCopy.exists()) {
        FileUtil.copy(originalIniCopy, originalIni);
      }
      // work around copyDirContent's copying of symlinks as hard links causing pants to fail
      assertTrue("Failed to clean up!", FileUtil.delete(new File(projectDir, ".pants.d")));
      // and IJ data
      assertTrue("Failed to clean up!", FileUtil.delete(new File(projectDir, ".idea")));
      for (File file : getProjectFoldersToCopy()) {
        final File[] children = file.listFiles();
        if (children == null) {
          continue;
        }
        for (File child : children) {
          final File copiedChild = new File(projectDir, child.getName());
          if (copiedChild.exists()) {
            assertTrue("Failed to clean up!", FileUtil.delete(copiedChild));
          }
        }
      }
    }
    else {
      cmd("git", "reset", "--hard");
      cmd("git", "clean", "-fdx");
    }
  }

  protected void cmd(File workDir, String... args) throws ExecutionException {
    final GeneralCommandLine commandLine = new GeneralCommandLine(args);
    final ProcessOutput cmdOutput = PantsUtil.getCmdOutput(commandLine.withWorkDirectory(workDir), null);
    assertTrue("Failed to execute: " + StringUtil.join(args, " "), cmdOutput.getExitCode() == 0);
  }

  protected void cmd(String... args) throws ExecutionException {
    final GeneralCommandLine commandLine = new GeneralCommandLine(args);
    final ProcessOutput cmdOutput = PantsUtil.getCmdOutput(commandLine.withWorkDirectory(getProjectFolder()), null);
    assertTrue("Failed to execute: " + StringUtil.join(args, " "), cmdOutput.getExitCode() == 0);
  }

  protected void killNailgun() throws ExecutionException {
    // NB: the ideal interface here is defaultCommandLine(myProject). However,
    // not all tests call doImport therefore myProject may not always contain modules.
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(getProjectPath());
    commandLine.addParameter("ng-killall");
    // Wait for command to finish.
    PantsUtil.getCmdOutput(commandLine, null);
  }

  @NotNull
  protected File getProjectFolder() {
    final String ossPantsHome = System.getenv("OSS_PANTS_HOME");
    if (!StringUtil.isEmpty(ossPantsHome)) {
      return new File(ossPantsHome);
    }
    final File workingDir = PantsTestUtils.findTestPath("testData").getParentFile();
    return new File(workingDir.getParent(), "pants");
  }

  @NotNull
  protected List<File> getProjectFoldersToCopy() {
    return Collections.emptyList();
  }

  @Override
  protected String getProjectPath() {
    return Paths.get(super.getProjectPath(), StringUtil.notNullize(myRelativeProjectPath)).toString();
  }

  @NotNull
  public CompilerTester getCompilerTester() throws Exception {
    // CompilerTester needs to be updated every time because project modules may change.
    if (myCompilerTester != null) {
      myCompilerTester.tearDown();
    }
    final List<Module> allModules = Arrays.asList(ModuleManager.getInstance(myProject).getModules());
    myCompilerTester = new CompilerTester(myProject, allModules);
    return myCompilerTester;
  }

  protected void assertProjectName(String name) {
    assertEquals(name, myProject.getName());
  }

  protected void assertScalaLibrary(String moduleName) throws Exception {
    // This is to match scala-platform used in pants repo across different releases,
    // so we expect at least one of the versions should be found here.
    ArrayList<String> expectedLibs =
      Lists.newArrayList(
        "Pants: org.scala-lang:scala-library:2.10.4",
        "Pants: org.scala-lang:scala-library:2.10.6",
        "Pants: org.scala-lang:scala-library:2.11.8",
        "Pants: org.scala-lang:scala-library:2.11.11"
      );
    for (String libName : expectedLibs) {
      LibraryOrderEntry libX = ContainerUtil.getFirstItem(this.getModuleLibDeps(moduleName, libName));
      if (libX != null) {
        assertModuleLibDep(moduleName, libName);
        return;
      }
    }
    fail(String.format("None of %s is found for module %s.", expectedLibs, moduleName));
  }

  protected void assertManifestJarExists() throws Exception {
    assertTrue(
      "Manifest jar not found.",
      findManifestJar().isPresent()
    );
  }

  private Optional<VirtualFile> findManifestJar() throws Exception {
    Optional<PantsOptions> pantsOptions = PantsOptions.getPantsOptions(myProject);
    if (!pantsOptions.isPresent()) {
      return Optional.empty();
    }
    if (pantsOptions.get().has(PantsConstants.PANTS_OPTION_EXPORT_CLASSPATH_MANIFEST_JAR)) {
      Optional<VirtualFile> manifestJar = PantsUtil.findProjectManifestJar(myProject);
      if (manifestJar.isPresent()) {
        return manifestJar;
      }
    }
    return Optional.empty();
  }

  protected void modify(@NonNls @NotNull String qualifiedName) {
    final PsiClass psiClass = findClassAndAssert(qualifiedName);
    final PsiFile psiFile = psiClass.getContainingFile();
    final PsiParserFacade parserFacade = PsiParserFacade.SERVICE.getInstance(myProject);
    final PsiComment comment = parserFacade.createBlockCommentFromText(psiFile.getLanguage(), "Foo");
    WriteCommandAction.runWriteCommandAction(
      myProject,
      new Runnable() {
        @Override
        public void run() {
          psiFile.add(comment);
        }
      }
    );
    FileDocumentManager manager = FileDocumentManager.getInstance();
    manager.saveAllDocuments();
  }

  @NotNull
  protected PsiClass findClassAndAssert(@NonNls @NotNull String qualifiedName) {
    final PsiClass[] classes = JavaPsiFacade.getInstance(myProject).findClasses(qualifiedName, GlobalSearchScope.allScope(myProject));
    assertTrue("Several classes with the same qualified name " + qualifiedName, classes.length < 2);
    assertTrue(qualifiedName + " class not found!", classes.length > 0);
    return classes[0];
  }

  protected void doImport(@NotNull String projectFolderPathToImport, String... targetNames) {
    System.out.println("Import: " + projectFolderPathToImport);
    myRelativeProjectPath = projectFolderPathToImport;
    myProjectSettings.setTargetSpecs(PantsUtil.convertToTargetSpecs(projectFolderPathToImport, Arrays.asList(targetNames)));
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        PantsUtil.getDefaultJavaSdk(getProjectPath())
          .ifPresent(sdk -> {
            ProjectJdkTable.getInstance().addJdk(sdk);
            NewProjectUtil.applyJdkToProject(myProject, sdk);
          });
      }
    });
    importProject();
    PantsMetrics.markIndexEnd();
  }

  protected void assertGotoFileContains(String filename) {
    final GotoFileModel gotoFileModel = new GotoFileModel(myProject);
    assertTrue(ArrayUtil.contains(filename, gotoFileModel.getNames(false)));
  }

  @Override
  protected void compileModules(String... moduleNames) {
    throw new AssertionError("Please use makeModules method instead!");
  }

  protected void assertCompilationFailed(final String... moduleNames) throws Exception {
    assertCompilationFailed(getModules(moduleNames));
  }

  protected void assertCompilationFailed(final Module... modules) throws Exception {
    final List<CompilerMessage> messages = compileAndGetMessages(modules);
    for (CompilerMessage message : messages) {
      if (message.getCategory() == CompilerMessageCategory.ERROR) {
        return;
      }
    }
    fail("Compilation didn't fail!\n" + messages);
  }

  protected List<CompilerMessage> compileAndGetMessages(Module... modules) throws Exception {
    final ModuleCompileScope scope = new ModuleCompileScope(myProject, modules, true);
    return getCompilerTester().make(scope);
  }

  private Module[] getModules(final String... moduleNames) {
    final List<Module> modules = new ArrayList<>();
    for (String name : moduleNames) {
      modules.add(getModule(name));
    }
    return modules.toArray(new Module[modules.size()]);
  }

  protected void assertFirstSourcePartyModules(String... expectedNames) {
    final Set<Module> sourceModules = Arrays.stream(ModuleManager.getInstance(myProject).getModules())
      .filter(PantsUtil::isSourceModule)
      .collect(Collectors.toSet());

    final Set<String> firstPartySourceModuleNames = sourceModules.stream()
      .map(Module::getName)
      .filter(moduleName -> !moduleName.startsWith(".pants.d") && !moduleName.startsWith("3rdparty"))
      .collect(Collectors.toSet());

    Set<String> expectedModuleNames = Arrays.stream(expectedNames).collect(Collectors.toSet());

    Set<String> inExpectedOnly = Sets.difference(expectedModuleNames, firstPartySourceModuleNames);
    Set<String> inFirstPartyOnly = Sets.difference(firstPartySourceModuleNames, expectedModuleNames);

    assertEquals(
      String.format("Only in expected: %s, only in actual: %s", inExpectedOnly, inFirstPartyOnly),
      expectedModuleNames,
      firstPartySourceModuleNames
    );
  }

  protected void assertModuleExists(String moduleName) {
    final List<String> moduleNames = Arrays.stream(ModuleManager.getInstance(myProject).getModules())
      .map(Module::getName)
      .collect(Collectors.toList());
    assertContain(moduleNames, moduleName);
  }

  protected void assertGenModules(int count) {
    final List<Module> genModules = ContainerUtil.findAll(
      ModuleManager.getInstance(myProject).getModules(),
      new Condition<Module>() {
        @Override
        public boolean value(Module module) {
          return module.getName().startsWith(".pants.d");
        }
      }
    );

    assertSize(count, genModules);
  }

  public void assertSuccessfulTest(String moduleName, String className) {
    final OSProcessHandler handler = runJUnitTest(moduleName, className, null);
    assertEquals("Bad exit code! Tests failed!", 0, handler.getProcess().exitValue());
  }

  public void assertSuccessfulTest(RunConfiguration configuration) {
    final OSProcessHandler handler = runWithConfiguration(configuration);
    assertEquals("Bad exit code! Tests failed!", 0, handler.getProcess().exitValue());
  }

  public OSProcessHandler runJUnitTest(String moduleName, String className, @Nullable String vmParams) {
    return runWithConfiguration(generateJUnitConfiguration(moduleName, className, vmParams));
  }

  @NotNull
  protected OSProcessHandler runWithConfiguration(RunConfiguration configuration) {
    final PantsJUnitRunnerAndConfigurationSettings runnerAndConfigurationSettings =
      new PantsJUnitRunnerAndConfigurationSettings(configuration);
    final ExecutionEnvironmentBuilder environmentBuilder =
      ExecutionUtil.createEnvironment(DefaultRunExecutor.getRunExecutorInstance(), runnerAndConfigurationSettings);
    final ExecutionEnvironment environment = environmentBuilder.build();

    ProgramRunnerUtil.executeConfiguration(environment, false, false);
    final OSProcessHandler handler = (OSProcessHandler) environment.getContentToReuse().getProcessHandler();
    assertTrue(handler.waitFor());
    return handler;
  }

  @NotNull
  protected JUnitConfiguration generateJUnitConfiguration(String moduleName, String className, @Nullable String vmParams) {
    final ConfigurationFactory factory = JUnitConfigurationType.getInstance().getConfigurationFactories()[0];
    final JUnitConfiguration runConfiguration = new JUnitConfiguration("Pants: " + className, myProject, factory);
    runConfiguration.setWorkingDirectory(PantsUtil.findBuildRoot(getModule(moduleName)).get().getCanonicalPath());
    runConfiguration.setModule(getModule(moduleName));
    runConfiguration.setName(className);
    if (StringUtil.isNotEmpty(vmParams)) {
      runConfiguration.setVMParameters(vmParams);
    }
    runConfiguration.setMainClass(findClassAndAssert(className));
    return runConfiguration;
  }

  /**
   * TODO: ScalaTestRunConfiguration setting may not be fully correct yet. Currently this can only be used to invoke scala runner,
   * but the run itself may not succeed.
   */
  @NotNull
  protected ScalaTestRunConfiguration generateScalaRunConfiguration(String moduleName, String className, @Nullable String vmParams) {
    final ConfigurationFactory factory = ScalaTestConfigurationType.CONFIGURATION_TYPE_EP.getExtensions()[0].getConfigurationFactories()[0];
    final ScalaTestRunConfiguration runConfiguration = new ScalaTestRunConfiguration(myProject, factory, className);
    runConfiguration.setWorkingDirectory(PantsUtil.findBuildRoot(getModule(moduleName)).get().getCanonicalPath());
    runConfiguration.setModule(getModule(moduleName));
    runConfiguration.setName(className);
    runConfiguration.setTestClassPath(className);
    return runConfiguration;
  }

  protected void gitResetRepoCleanExampleDistDir() throws ExecutionException {
    // Git reset .cache/pants dir
    cmd("git", "reset", "--hard");
    // Only the files under examples are going to be modified.
    // Hence issue `git clean -fdx` under examples, so pants does not
    // have to bootstrap again.
    File exampleDir = new File(getProjectFolder(), "examples");
    cmd(exampleDir, "git", "clean", "-fdx");
    cmd("rm", "-rf", "dist");
  }

  @Override
  public void tearDown() throws Exception {
    // TODO thread leak either a IJ bug https://youtrack.jetbrains.com/issue/IDEA-155496
    // or a pants plugin bug https://github.com/pantsbuild/intellij-pants-plugin/issues/130
    // Temporarily ignore the following 'leaking' threads to pass CI.
    ThreadTracker.longRunningThreadCreated(
      ApplicationManager.getApplication(),
      "BaseDataReader",
      "ProcessWaitFor",
      "Timer"
    );
    try {
      if (myCompilerTester != null) {
        myCompilerTester.tearDown();
      }

      // Kill nailgun after usage as memory on travis is limited, at a cost of slower later builds.
      killNailgun();
      cleanProjectRoot();
      Messages.setTestDialog(TestDialog.DEFAULT);
      super.tearDown();
    }
    catch (Throwable throwable) {
      // Discard error containing "Already disposed".
      if (!throwable.getMessage().contains("Already disposed")) {
        throw throwable;
      }
    }
  }

  @Override
  protected void importProject(@NonNls @Language("Python") String config) throws IOException {
    super.importProject(config);
  }

  @Override
  protected ExternalProjectSettings getCurrentExternalProjectSettings() {
    return myProjectSettings;
  }

  @Override
  protected ProjectSystemId getExternalSystemId() {
    return PantsConstants.SYSTEM_ID;
  }

  @Override
  protected String getTestsTempDir() {
    return "pants";
  }

  @Override
  protected String getExternalSystemConfigFileName() {
    return "BUILD";
  }

  protected void assertAndRunPantsMake(JUnitConfiguration runConfiguration) {

    RunManager runManager = RunManager.getInstance(myProject);
    assertTrue(runManager instanceof RunManagerImpl);
    RunManagerImpl runManagerImpl = (RunManagerImpl) runManager;

    RunnerAndConfigurationSettings runnerAndConfigurationSettings =
      runManagerImpl.createConfiguration(runConfiguration, JUnitConfigurationType.getInstance().getConfigurationFactories()[0]);
    runManagerImpl.addConfiguration(runnerAndConfigurationSettings, false);

    // Make sure PantsMake is the one and only task before JUnit run.
    List<BeforeRunTask<?>> beforeRunTaskList = runManagerImpl.getBeforeRunTasks(runConfiguration);
    assertEquals(1, beforeRunTaskList.size());
    BeforeRunTask task = beforeRunTaskList.iterator().next();
    assertEquals(PantsMakeBeforeRun.ID, task.getProviderId());

    /*
     * Manually invoke BeforeRunTask as {@link ExecutionManager#compileAndRun} launches another task asynchronously,
     * and there is no way to catch that.
     */
    BeforeRunTaskProvider provider = BeforeRunTaskProvider.getProvider(myProject, task.getProviderId());
    assertNotNull(String.format("Cannot find BeforeRunTaskProvider for id='%s'", task.getProviderId()), provider);
    assertTrue(provider.executeTask(null, runConfiguration, null, task));
  }

  protected PantsMakeBeforeRun.PantsExecuteTaskResult pantsCompileProject() {
    PantsMakeBeforeRun runner = new PantsMakeBeforeRun(myProject);
    return runner.executeCompileTask(myProject);
  }

  protected void assertPantsCompileExecutesAndSucceeds(final PantsMakeBeforeRun.PantsExecuteTaskResult compileResult) throws Exception {
    assertTrue("Compile failed", compileResult.succeeded);
    if (compileResult.output.isPresent()) {
      assertTrue("Compile was noop, but should not be.", !PantsConstants.NOOP_COMPILE.equals(compileResult.output.get()));
    }
    assertManifestJarExists();
  }

  protected void assertPantsCompileNoop(final PantsMakeBeforeRun.PantsExecuteTaskResult compileResult) throws Exception {
    assertTrue("Compile failed.", compileResult.succeeded);
    assertTrue("Compile message not found.", compileResult.output.isPresent());
    assertEquals("Compile was not noop, but should be.", PantsConstants.NOOP_COMPILE, compileResult.output.get());
    assertManifestJarExists();
  }

  protected void assertPantsCompileFailure(final PantsMakeBeforeRun.PantsExecuteTaskResult compileResult) {
    assertFalse("Compile succeeded, but should fail.", compileResult.succeeded);
  }

  protected PantsMakeBeforeRun.PantsExecuteTaskResult pantsCompileModule(String... moduleNames) {
    PantsMakeBeforeRun runner = new PantsMakeBeforeRun(myProject);
    return runner.executeCompileTask(getModules(moduleNames));
  }
}
