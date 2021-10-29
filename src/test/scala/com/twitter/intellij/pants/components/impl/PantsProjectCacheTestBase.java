// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.components.PantsProjectCache;
import com.twitter.intellij.pants.testFramework.PantsCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public abstract class PantsProjectCacheTestBase extends PantsCodeInsightFixtureTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    cleanUpsourceFolders();
  }

  private void cleanUpsourceFolders() {
    ApplicationManager.getApplication().runWriteAction(
      () -> {
        final ModifiableRootModel modifiableRootModel = ModuleRootManager.getInstance(getModule()).getModifiableModel();
        for (ContentEntry contentEntry : modifiableRootModel.getContentEntries()) {
          final SourceFolder[] sourceFolders = contentEntry.getSourceFolders();
          for (SourceFolder sourceFolder : sourceFolders) {
            contentEntry.removeSourceFolder(sourceFolder);
          }
        }
        modifiableRootModel.commit();
      }
    );
  }

  @Override
  protected void tearDown() throws Exception {
    cleanUpsourceFolders();
    super.tearDown();
  }

  @NotNull
  protected VirtualFile getMainContentRoot() {
    final VirtualFile result = ArrayUtil.getFirstElement(ModuleRootManager.getInstance(getModule()).getContentRoots());
    assertNotNull(result);
    return result;
  }

}
