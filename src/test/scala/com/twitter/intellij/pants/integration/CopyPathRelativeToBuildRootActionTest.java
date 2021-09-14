// Copyright 2018 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.StubVirtualFile;
import com.intellij.testFramework.MapDataContext;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.ui.CopyPathRelativeToBuildRootAction;
import org.jetbrains.annotations.NotNull;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.HashMap;

public class CopyPathRelativeToBuildRootActionTest extends OSSPantsIntegrationTest {
  public void testCompileTargetsInSelectedEditor() throws Throwable {
    String projectFolderPath = "examples/tests/scala/org/pantsbuild/example";
    doImport(projectFolderPath);
    String projectRelativePath = projectFolderPath + "/hello/greet/Greeting.java";
    VirtualFile virtualFile = new StubVirtualFile() {
      @NotNull
      @Override
      public String getPath() {
        return new File(getProjectFolder(), projectRelativePath).getAbsolutePath();
      }
    };
    AnActionEvent event = AnActionEvent.createFromDataContext(
      "",
      null,
      makeDataContext(myProject, virtualFile)
    );

    new CopyPathRelativeToBuildRootAction().actionPerformed(event);
    assertEquals(projectRelativePath, Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor));
  }

  private DataContext makeDataContext(Project project, VirtualFile file) {
    HashMap<DataKey<?>, Object> map = new HashMap<DataKey<?>, Object>() {{
      put(CommonDataKeys.PROJECT, project);
      put(CommonDataKeys.VIRTUAL_FILE, file);
    }};

    return new MapDataContext(map);
  }
}
