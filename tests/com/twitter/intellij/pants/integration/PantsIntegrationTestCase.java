// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.compiler.impl.ModuleCompileScope;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.test.ExternalSystemImportingTestCase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.CompilerTester;
import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class PantsIntegrationTestCase extends ExternalSystemImportingTestCase {
  private PantsProjectSettings myProjectSettings;
  private String myRelativeProjectPath;
  private CompilerTester myCompilerTester;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myProjectSettings = new PantsProjectSettings();
    myProjectSettings.setAllTargets(true);
    myCompilerTester = null;
  }

  @Override
  protected void setUpInWriteAction() throws Exception {
    super.setUpInWriteAction();
    final File projectTemplateFolder = getProjectFolderToCopy();
    if (!projectTemplateFolder.exists() || !projectTemplateFolder.isDirectory()) {
      fail("invalid template project path " + projectTemplateFolder.getAbsolutePath());
    }
    FileUtil.copyDirContent(projectTemplateFolder, new File(myProjectRoot.getPath()));
  }

  abstract protected File getProjectFolderToCopy();

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

  protected void doImport(String projectFolderPathToImport) {
    myRelativeProjectPath = projectFolderPathToImport;
    importProject();
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
        final String prettyMessage =
          String.format(
            "%s at %s:%s",
            message.getMessage(), message.getVirtualFile().getCanonicalPath(), message.getRenderTextPrefix()
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
    try {
      if (myCompilerTester != null) {
        myCompilerTester.tearDown();
      }
      Messages.setTestDialog(TestDialog.DEFAULT);
    }
    finally {
      super.tearDown();
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
      final VirtualFile moduleOutput = ModuleRootManager.getInstance(module).getModuleExtension(CompilerModuleExtension.class).getCompilerOutputPath();
      assert moduleOutput != null;
      moduleOutput.refresh(false, true);
      return moduleOutput.findFileByRelativePath(className.replace('.', '/') + ".class");
    }
  }
}
