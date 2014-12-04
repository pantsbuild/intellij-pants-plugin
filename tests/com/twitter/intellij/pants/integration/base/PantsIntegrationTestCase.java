// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration.base;

import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.util.gotoByName.GotoFileModel;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.test.ExternalSystemImportingTestCase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.CompilerTester;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class PantsIntegrationTestCase extends ExternalSystemImportingTestCase {
  private static final String PLUGINS_KEY = "idea.load.plugins.id";

  private PantsProjectSettings myProjectSettings;
  private String myRelativeProjectPath;
  private CompilerTester myCompilerTester;
  private String defaultPlugins = null;

  @Override
  public void setUp() throws Exception {
    defaultPlugins = System.getProperty(PLUGINS_KEY);
    final String pluginIdsToInstall = StringUtil.join(getRequiredPluginIds(), ",");
    if (StringUtil.isNotEmpty(pluginIdsToInstall)) {
      System.setProperty(PLUGINS_KEY, pluginIdsToInstall + "," + defaultPlugins);
    }

    super.setUp();

    for (String pluginId : getRequiredPluginIds()) {
      final IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId(pluginId));
      assertNotNull(pluginId + " plugin should be in classpath for integration tests", plugin);
      if (!plugin.isEnabled()) {
        assertTrue(PluginManagerCore.enablePlugin(pluginId));
      }
    }

    myProjectSettings = new PantsProjectSettings();
    myProjectSettings.setAllTargets(true);
    myCompilerTester = null;
  }

  protected String[] getRequiredPluginIds() {
    return new String[]{ "org.intellij.scala" };
  }

  @Override
  protected void setUpInWriteAction() throws Exception {
    super.setUpInWriteAction();

    final File projectDir = new File(myProjectRoot.getPath());
    for (File projectTemplateFolder : getProjectFoldersToCopy()) {
      if (!projectTemplateFolder.exists() || !projectTemplateFolder.isDirectory()) {
        fail("invalid template project path " + projectTemplateFolder.getAbsolutePath());
      }

      FileUtil.copyDirContent(projectTemplateFolder, projectDir);
    }
    // work around copyDirContent's copying of symlinks as hard links causing pants to fail
    FileUtil.delete(new File(myProjectRoot.getPath() + "/.pants.d/runs/latest"));
    FileUtil.delete(new File(myProjectRoot.getPath() + "/.pants.d/reports/latest"));
  }

  abstract protected List<File> getProjectFoldersToCopy();

  @Override
  protected String getProjectPath() {
    return super.getProjectPath() + "/" + StringUtil.notNullize(myRelativeProjectPath);
  }

  @Nullable
  public CompilerTester getCompilerTester() {
    return myCompilerTester;
  }

  @Nullable
  protected VirtualFile findClassFile(String className, String moduleName) {
    assertNotNull("Compilation wasn't completed successfully!", getCompilerTester());
    return getCompilerTester().findClassFile(className, getModule(moduleName));
  }

  @Nullable
  protected PsiClass findClass(@NonNls @NotNull String qualifiedName) {
    PsiClass[] classes = JavaPsiFacade.getInstance(myProject).findClasses(qualifiedName, GlobalSearchScope.allScope(myProject));
    assertTrue(classes.length < 2);
    return classes.length > 0 ? classes[0] : null;
  }

  protected void doImport(String projectFolderPathToImport) {
    myRelativeProjectPath = projectFolderPathToImport;
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

  /**
   * We don't use com.intellij.openapi.externalSystem.test.ExternalSystemTestCase#compileModules
   * because we want to do some assertions on myCompilerTester
   */
  protected void makeModules(final String... moduleNames) {
    make(createModulesCompileScope(moduleNames));
  }

  private void make(final CompileScope scope) {
    try {
      myCompilerTester = new MyCompilerTester(myProject, Arrays.asList(scope.getAffectedModules()));
      final List<CompilerMessage> messages = myCompilerTester.make(scope);
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
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private CompileScope createModulesCompileScope(final String... moduleNames) {
    final List<Module> modules = new ArrayList<Module>();
    for (String name : moduleNames) {
      modules.add(getModule(name));
    }
    return new ModuleCompileScope(myProject, modules.toArray(new Module[modules.size()]), true);
  }

  @Override
  public void tearDown() throws Exception {
    if (defaultPlugins != null) {
      System.setProperty(PLUGINS_KEY, defaultPlugins);
      defaultPlugins = null;
    }
    try {
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

  private static class MyCompilerTester extends CompilerTester {
    public MyCompilerTester(Project project, List<Module> modules) throws Exception {
      super(project, modules);
    }

    /**
     * Override because the super method is incorrect.
     * Super method uses findChild instead of findFileByRelativePath.
     */
    @Nullable
    @Override
    public VirtualFile findClassFile(String className, Module module) {
      final VirtualFile moduleOutput =
        ModuleRootManager.getInstance(module).getModuleExtension(CompilerModuleExtension.class).getCompilerOutputPath();
      assert moduleOutput != null;
      moduleOutput.refresh(false, true);
      return moduleOutput.findFileByRelativePath(className.replace('.', '/') + ".class");
    }
  }
}
