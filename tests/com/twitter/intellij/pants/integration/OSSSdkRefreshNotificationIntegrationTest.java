// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationsImpl;
import com.twitter.intellij.pants.file.ModifiedSdkNotificationProvider;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsSdkUtil;

import java.nio.file.Files;
import java.nio.file.Path;


public class OSSSdkRefreshNotificationIntegrationTest extends OSSPantsIntegrationTest {

  public void testPromptAfterJdkChanges() throws Throwable {
    Path sdks = Files.createTempDirectory("test-sdk-");
    Path sdkLink = sdks.resolve("link");

    Path oldSdk = Files.createDirectory(sdks.resolve("old"));
    Files.createDirectories(oldSdk.resolve("jre/lib"));
    Files.createFile(oldSdk.resolve("jre/lib/foo.jar"));

    Path newSdk = Files.createDirectory(sdks.resolve("new"));
    Files.createDirectories(newSdk.resolve("jre/lib"));
    Files.createFile(newSdk.resolve("jre/lib/bar.jar"));

    Files.createSymbolicLink(sdkLink, oldSdk);

    try {
      FileEditor editor = ApplicationManager.getApplication()
        .runWriteAction((ThrowableComputable<FileEditor, Throwable>) () -> {
          doImport("examples/tests/java/org/pantsbuild/example/useproto");
          // setup sdk
          Sdk sdk = PantsSdkUtil.createAndRegisterJdk("SDK", sdkLink.toString(), getTestRootDisposable());
          ProjectRootManager.getInstance(myProject).setProjectSdk(sdk);

          // open file
          VirtualFile file = firstMatchingVirtualFileInProject("Distances.java");
          FileEditor[] editors = FileEditorManager.getInstance(myProject).openFile(file, true);

          // change link to point to the new sdk
          Files.delete(sdkLink);
          Files.createSymbolicLink(sdkLink, newSdk);
          LocalFileSystem.getInstance().refresh(false);
          return editors[0];
        });

      // verify notification panel is shown to the user
      EditorNotificationsImpl.completeAsyncTasks();
      EditorNotificationPanel panel = editor.getUserData(ModifiedSdkNotificationProvider.KEY);
      assertNotNull(panel);
    }
    finally {
      removeJdks(sdk -> sdk.getName().equals("SDK"));
    }
  }

  @Override
  public void tearDown() throws Exception {
    gitResetRepoCleanExampleDistDir();
    super.tearDown();
  }
}
