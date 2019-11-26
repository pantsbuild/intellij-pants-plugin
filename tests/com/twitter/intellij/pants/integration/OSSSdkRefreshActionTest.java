// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.google.common.collect.Sets;
import com.intellij.ide.impl.NewProjectUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.ui.PantsSdkRefreshAction;
import com.twitter.intellij.pants.util.PantsSdkUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotEquals;


public class OSSSdkRefreshActionTest extends OSSPantsIntegrationTest {

  private final DataContext PANTS_PROJECT_DATA = s -> s.equals("project") ? myProject : null;

  public void testRefreshSdkWhenNotSet() {
    doImport("examples/tests/java/org/pantsbuild/example/useproto");

    ApplicationManager.getApplication().runWriteAction(() -> {
      ProjectRootManager.getInstance(myProject).setProjectSdk(null);
    });

    refreshProjectSdk();
    Sdk sdk = ProjectRootManager.getInstance(myProject).getProjectSdk();
    assertNotEmpty(runtimeOf(sdk));
  }

  public void testRefreshPantsSdk() {
    ProjectRootManager rootManager = ProjectRootManager.getInstance(myProject);

    Sdk sdk = createDummySdk("1.8_from_" + pantsExecutable());
    String originalSdkPath = sdk.getHomePath();

    doImport("examples/tests/java/org/pantsbuild/example/useproto");
    assertEquals(originalSdkPath, rootManager.getProjectSdk().getHomePath());

    refreshProjectSdk();

    // the SDK gets updated
    assertSame(sdk, rootManager.getProjectSdk());
    assertNotEquals(originalSdkPath, sdk.getHomePath());
  }

  public void testRefreshNonPantsSdk() {
    String customJdkName = "Custom-JDK";

    try {
      Application application = ApplicationManager.getApplication();
      ProjectRootManager rootManager = ProjectRootManager.getInstance(myProject);

      Sdk customSdk = createDummySdk(customJdkName);

      doImport("examples/tests/java/org/pantsbuild/example/useproto");

      // set custom SDK
      application.runWriteAction(() -> {
        NewProjectUtil.applyJdkToProject(myProject, customSdk);
      });
      assertEquals(customJdkName, rootManager.getProjectSdk().getName());

      refreshProjectSdk();

      // refreshing changes the project SDK
      Sdk pantsSdk = rootManager.getProjectSdk();
      assertNotSame(customSdk, pantsSdk);

      // previously used custom SDK is not removed
      HashSet<Sdk> jdks = Sets.newHashSet(ProjectJdkTable.getInstance().getAllJdks());
      assertContainsElements(jdks, customSdk, pantsSdk);
    }
    finally {
      removeJdks(jdk -> jdk.getName().equals(customJdkName));
    }
  }

  private Set<VirtualFile> runtimeOf(Sdk sdk) {
    return Sets.newHashSet(sdk.getRootProvider().getFiles(OrderRootType.CLASSES));
  }

  @NotNull
  private Sdk createDummySdk(String sdkName) {
    try {
      Path sdkPath = Files.createTempDirectory("test-sdk-");
      Files.createDirectories(sdkPath.resolve("jre/lib"));
      Files.createFile(sdkPath.resolve("jre/lib/foo.jar"));
      return PantsSdkUtil.createAndRegisterJdk(sdkName, sdkPath.toString(), getTestRootDisposable());
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String pantsExecutable() {
    String projectPath = getProjectPath();
    return PantsUtil.findPantsExecutable(projectPath).get().getPath();
  }

  private void refreshProjectSdk() {
    ActionManager.getInstance().getAction(PantsSdkRefreshAction.class.getName())
      .actionPerformed(AnActionEvent.createFromDataContext("", null, PANTS_PROJECT_DATA));
  }

  @Override
  public void tearDown() throws Exception {
    removeJdks(jdk -> jdk.getName().contains(pantsExecutable()));
    gitResetRepoCleanExampleDistDir();
    super.tearDown();
  }
}
