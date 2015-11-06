// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework;

import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.util.gotoByName.GotoFileModel;
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
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
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
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * If your integration test modifies any source files
 * please set {@link PantsIntegrationTestCase#readOnly} to false.
 *
 * @see com.twitter.intellij.pants.highlighting.PantsHighlightingIntegrationTest
 */
public abstract class PantsIntegrationTestCase extends ExternalSystemImportingTestCase {
  private static final String PANTS_COMPILER_ENABLED = "pants.compiler.enabled";
  private static final String isolatedPantsIniName = "pants.ini.isolated";

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
    for (String pluginId : getRequiredPluginIds()) {
      final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(pluginId));
      assertNotNull(pluginId + " plugin should be in classpath for integration tests!", plugin);
      assertTrue(pluginId + " is not enabled!", plugin.isEnabled());
    }

    final boolean usePantsToCompile = Boolean.valueOf(System.getProperty(PANTS_COMPILER_ENABLED, "true"));
    PantsSettings.getInstance(myProject).setCompileWithIntellij(!usePantsToCompile);

    myProjectSettings = new PantsProjectSettings();
    myCompilerTester = null;
  }

  protected String[] getRequiredPluginIds() {
    return new String[]{ "org.intellij.scala" };
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


    final File isolatedIni = new File(projectDir, isolatedPantsIniName);
    // isolatedIni doesn't exist after global strategy was removed.
    if (PantsUtil.isIsolatedStrategyTestFlagEnabled() && isolatedIni.exists()) {
      final File originalIni = new File(projectDir, "pants.ini");
      final File originalIniCopy = new File(projectDir, "pants.ini.copy");
      FileUtil.copy(originalIni, originalIniCopy);
      // There is no way to pass env vars to com.twitter.intellij.pants.jps.incremental.PantsTargetBuilder.
      // So let's just emulate the override.
      FileUtil.appendToFile(originalIni, "\n" + FileUtil.loadFile(isolatedIni));
    }
  }

  private void cleanProjectRoot() throws ExecutionException, IOException {
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
    } else {
      cmd("git", "reset", "--hard");
      cmd("git", "clean", "-fdx");
    }
  }

  private void cmd(String ...args) throws ExecutionException {
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
    if (myCompilerTester == null) {
      final List<Module> allModules = Arrays.asList(ModuleManager.getInstance(myProject).getModules());
      myCompilerTester = new CompilerTester(myProject, allModules);
    }
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
    final Module module = getModule(moduleName);
    assertNotNull("Compilation wasn't completed successfully!", getCompilerTester());
    List<String> compilerOutputPaths = ContainerUtil.newArrayList();
    if (PantsSettings.getInstance(myProject).isCompileWithIntellij()) {
      final CompilerModuleExtension moduleExtension =
        ModuleRootManager.getInstance(module).getModuleExtension(CompilerModuleExtension.class);
      compilerOutputPaths.add(VfsUtil.urlToPath(moduleExtension.getCompilerOutputUrl()));
      compilerOutputPaths.add(VfsUtil.urlToPath(moduleExtension.getCompilerOutputUrlForTests()));
    } else {
      compilerOutputPaths.addAll(StringUtil.split(module.getOptionValue(PantsConstants.PANTS_COMPILER_OUTPUTS_KEY), ":"));
      compilerOutputPaths.addAll(PantsClasspathRunConfigurationExtension.findPublishedClasspath(module));
    }
    for (String compilerOutputPath : compilerOutputPaths) {
      VirtualFile output = VirtualFileManager.getInstance().refreshAndFindFileByUrl(VfsUtil.pathToUrl(compilerOutputPath));
      if (output == null) {
        continue;
      }
      if (StringUtil.equalsIgnoreCase(output.getExtension(), "jar")) {
        output = JarFileSystem.getInstance().getJarRootForLocalFile(output);
        assertNotNull(output);
      }
      final VirtualFile classFile = output.findFileByRelativePath(className.replace('.', '/') + ".class");
      if (classFile != null) {
        return classFile;
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
    if (!PantsSettings.getInstance(myProject).isCompileWithIntellij()) {
      final String noChanges = "pants: No changes to compile.";
      final String compiledSuccessfully = "pants: SUCCESS";
      assertTrue("Compilation wasn't successful!", rawMessages.contains(noChanges) || rawMessages.contains(compiledSuccessfully));
    }
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
   *  Same as super method except it doesn't check for gen modules.
   *  It appeared names of gen modules are changing from time to time
   *  and we can't use a determenistic one because we run tests
   *  for different version of pants.
   *
   *  Use assertGenModules instead.
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


  @Override
  public void tearDown() throws Exception {
    try {
      cleanProjectRoot();
      if (myCompilerTester != null) {
        myCompilerTester.tearDown();
      }
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
