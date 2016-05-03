// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ExecutionUtil;
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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.CompilerTester;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.execution.PantsClasspathRunConfigurationExtension;
import com.twitter.intellij.pants.model.PantsOptions;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;

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
    return new String[]{"org.intellij.scala"};
  }

  @Override
  protected void setUpInWriteAction() throws Exception {
    super.setUpInWriteAction();

    final Sdk sdk = JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
    ProjectRootManager.getInstance(myProject).setProjectSdk(sdk);

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

  private void cmd(String... args) throws ExecutionException {
    final GeneralCommandLine commandLine = new GeneralCommandLine(args);
    final ProcessOutput cmdOutput = PantsUtil.getCmdOutput(commandLine.withWorkDirectory(getProjectFolder()), null);
    assertTrue("Failed to execute: " + StringUtil.join(args, " "), cmdOutput.getExitCode() == 0);
  }

  @NotNull
  abstract protected File getProjectFolder();

  @NotNull
  protected List<File> getProjectFoldersToCopy() {
    return Collections.emptyList();
  }

  @Override
  protected String getProjectPath() {
    return super.getProjectPath() + "/" + StringUtil.notNullize(myRelativeProjectPath);
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

  protected void assertScalaLibrary(String moduleName) throws Exception {
    assertModuleLibDep(moduleName, "Pants: org.scala-lang:scala-library:2.10.4");
  }

  protected void assertClassFileInModuleOutput(String className, String moduleName) throws Exception {
    assertNotNull(
      String.format("Didn't find %s class in %s module's output!", className, moduleName),
      findClassFile(className, moduleName)
    );
  }

  @Nullable
  private VirtualFile findClassFile(String className, String moduleName) throws Exception {
    PantsOptions pantsOptions = PantsOptions.getPantsOptions(myProject);
    if (pantsOptions == null) {
      return null;
    }
    if (pantsOptions.has(PantsConstants.PANTS_OPTION_EXPORT_CLASSPATH_MANIFEST_JAR)) {
      VirtualFile manifestJar = PantsUtil.findProjectManifestJar(myProject);
      if (manifestJar != null) {
        return manifestJar;
      }
      return null;
    }

    ApplicationManager.getApplication().runWriteAction(
      new Runnable() {
        @Override
        public void run() {
          // we need to refresh because IJ might not pick all newly created symlinks
          VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
        }
      }
    );

    final Module module = getModule(moduleName);
    final VirtualFile buildRoot = PantsUtil.findBuildRoot(module);
    assertNotNull("Can't find working dir for module '" + moduleName + "'!", buildRoot);
    assertNotNull("Compilation wasn't completed successfully!", getCompilerTester());
    List<String> compilerOutputPaths = ContainerUtil.newArrayList();

    VirtualFile pantsExecutable = PantsUtil.findPantsExecutable(PantsUtil.findBuildRoot(module).getPath());
    compilerOutputPaths.addAll(
      PantsClasspathRunConfigurationExtension.findPublishedClasspath(module, PantsUtil.hasTargetIdInExport(pantsExecutable.getPath())));

    for (String compilerOutputPath : compilerOutputPaths) {
      VirtualFile output = VirtualFileManager.getInstance().refreshAndFindFileByUrl(VfsUtil.pathToUrl(compilerOutputPath));
      if (output == null) {
        continue;
      }
      try {
        if (StringUtil.equalsIgnoreCase(output.getExtension(), "jar")) {
          output = JarFileSystem.getInstance().getJarRootForLocalFile(output);
          assertNotNull(output);
        }
        final VirtualFile classFile = output.findFileByRelativePath(className.replace('.', '/') + ".class");
        if (classFile != null) {
          return classFile;
        }
      }
      catch (AssertionError assertionError) {
        // There are some access assertions in tests. Ignore them.
        assertionError.printStackTrace();
      }
    }
    return null;
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
  }

  @NotNull
  protected PsiClass findClassAndAssert(@NonNls @NotNull String qualifiedName) {
    final PsiClass[] classes = JavaPsiFacade.getInstance(myProject).findClasses(qualifiedName, GlobalSearchScope.allScope(myProject));
    assertTrue("Several classes with the same qualified name " + qualifiedName, classes.length < 2);
    assertTrue(qualifiedName + " class not found!", classes.length > 0);
    return classes[0];
  }

  protected void doImportWithDependees(@NotNull String projectFolderPathToImport) {
    myProjectSettings.setWithDependees(true);
    doImport(projectFolderPathToImport);
  }

  protected void doImport(@NotNull String projectFolderPathToImport, String... targetNames) {
    myRelativeProjectPath = projectFolderPathToImport;
    myProjectSettings.setTargetNames(Arrays.asList(targetNames));
    importProject();
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

  /**
   * We don't use com.intellij.openapi.externalSystem.test.ExternalSystemTestCase#compileModules
   * because we want to do some assertions on myCompilerTester
   */
  protected List<String> makeModules(final String... moduleNames) throws Exception {
    return compile(getModules(moduleNames));
  }

  protected List<String> makeProject() throws Exception {
    return assertCompilerMessages(getCompilerTester().make());
  }

  protected List<String> compile(Module... modules) throws Exception {
    return assertCompilerMessages(compileAndGetMessages(modules));
  }

  private List<String> assertCompilerMessages(List<CompilerMessage> messages) {
    for (CompilerMessage message : messages) {
      final VirtualFile virtualFile = message.getVirtualFile();
      final String prettyMessage =
        virtualFile == null ?
        message.getMessage() :
        String.format(
          "%s at %s:%s", message.getMessage(), virtualFile.getCanonicalPath(), message.getRenderTextPrefix()
        );
      switch (message.getCategory()) {
        case ERROR:
          fail("Compilation failed with error: " + prettyMessage);
          break;
        case WARNING:
          System.out.println("Compilation warning: " + prettyMessage);
          break;
        case INFORMATION:
          break;
        case STATISTICS:
          break;
      }
    }
    final List<String> rawMessages = ContainerUtil.map(
      messages,
      new Function<CompilerMessage, String>() {
        @Override
        public String fun(CompilerMessage message) {
          return message.getMessage();
        }
      }
    );

    final String noChanges = "pants_plugin: " + PantsConstants.COMPILE_MESSAGE_NO_CHANGES_TO_COMPILE;
    final String compiledSuccessfully = "pants: SUCCESS";
    assertTrue("Compilation wasn't successful!", rawMessages.contains(noChanges) || rawMessages.contains(compiledSuccessfully));
    return rawMessages;
  }

  protected List<CompilerMessage> compileAndGetMessages(Module... modules) throws Exception {
    final ModuleCompileScope scope = new ModuleCompileScope(myProject, modules, true);
    return getCompilerTester().make(scope);
  }

  private Module[] getModules(final String... moduleNames) {
    final List<Module> modules = new ArrayList<Module>();
    for (String name : moduleNames) {
      modules.add(getModule(name));
    }
    return modules.toArray(new Module[modules.size()]);
  }

  /**
   * Same as super method except it doesn't check for gen modules.
   * It appeared names of gen modules are changing from time to time
   * and we can't use a determenistic one because we run tests
   * for different version of pants.
   * <p/>
   * Use assertGenModules instead.
   */
  @Override
  protected void assertModules(String... expectedNames) {
    final Module[] actual = ModuleManager.getInstance(myProject).getModules();
    final List<String> moduleNames = ContainerUtil.mapNotNull(
      actual,
      new Function<Module, String>() {
        @Override
        public String fun(Module module) {
          final String moduleName = module.getName();
          return moduleName.startsWith(".pants.d") || moduleName.startsWith("3rdparty") ? null : moduleName;
        }
      }
    );

    assertUnorderedElementsAreEqual(moduleNames, expectedNames);
  }

  protected void assertModuleExists(String moduleName) {
    final List<String> moduleNames = ContainerUtil.mapNotNull(
      ModuleManager.getInstance(myProject).getModules(),
      new Function<Module, String>() {
        @Override
        public String fun(Module module) {
          return module.getName();
        }
      }
    );

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

  public void assertSuccessfulJUnitTest(String moduleName, String className) {
    final OSProcessHandler handler = runJUnitTest(moduleName, className, null);
    assertEquals("Bad exit code! Tests failed!", 0, handler.getProcess().exitValue());
  }

  public void assertSuccessfulJUnitTest(JUnitConfiguration configuration) {
    final OSProcessHandler handler = runJUnitWithConfiguration(configuration);
    assertEquals("Bad exit code! Tests failed!", 0, handler.getProcess().exitValue());
  }

  public OSProcessHandler runJUnitTest(String moduleName, String className, @Nullable String vmParams) {
    return runJUnitWithConfiguration(generateJUnitConfiguration(moduleName, className, vmParams));
  }

  @NotNull
  private OSProcessHandler runJUnitWithConfiguration(JUnitConfiguration configuration) {
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
  public JUnitConfiguration generateJUnitConfiguration(String moduleName, String className, @Nullable String vmParams) {
    final ConfigurationFactory factory = JUnitConfigurationType.getInstance().getConfigurationFactories()[0];
    final JUnitConfiguration runConfiguration = new JUnitConfiguration("Pants: " + className, myProject, factory);
    runConfiguration.setWorkingDirectory(PantsUtil.findBuildRoot(getModule(moduleName)).getCanonicalPath());
    runConfiguration.setModule(getModule(moduleName));
    runConfiguration.setName(className);
    if (StringUtil.isNotEmpty(vmParams)) {
      runConfiguration.setVMParameters(vmParams);
    }
    runConfiguration.setMainClass(findClassAndAssert(className));
    return runConfiguration;
  }

  @Override
  public void tearDown() throws Exception {
    try {
      if (myCompilerTester != null) {
        myCompilerTester.tearDown();
      }
      cleanProjectRoot();
      Messages.setTestDialog(TestDialog.DEFAULT);
    }
    finally {
      super.tearDown();
      // double check.
      if (myProject != null && !myProject.isDisposed()) {
        Disposer.dispose(myProject);
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
}
