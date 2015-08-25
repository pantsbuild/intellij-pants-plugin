// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.openapi.application.AccessToken;
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

public class PantsProjectCacheTest extends PantsCodeInsightFixtureTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ApplicationManager.getApplication().runWriteAction(
      new Runnable() {
        @Override
        public void run() {
          final ModifiableRootModel modifiableRootModel = ModuleRootManager.getInstance(myModule).getModifiableModel();
          for (ContentEntry contentEntry : modifiableRootModel.getContentEntries()) {
            final SourceFolder[] sourceFolders = contentEntry.getSourceFolders();
            for (SourceFolder sourceFolder : sourceFolders) {
              contentEntry.removeSourceFolder(sourceFolder);
            }
          }
          modifiableRootModel.commit();
        }
      }
    );
    PantsProjectCacheImpl.getInstance(getProject()).projectOpened();
  }

  @Override
  protected void tearDown() throws Exception {
    PantsProjectCacheImpl.getInstance(getProject()).projectClosed();
    super.tearDown();
  }

  @NotNull
  public VirtualFile getMainContentRoot() {
    final VirtualFile result = ArrayUtil.getFirstElement(ModuleRootManager.getInstance(myModule).getContentRoots());
    assertNotNull(result);
    return result;
  }

  public void testEmpty() {
    final PantsProjectCache cache = PantsProjectCacheImpl.getInstance(myFixture.getProject());
    assertFalse(cache.folderContainsSourceRoot(myFixture.getProject().getBaseDir()));
  }

  public void testLastOne() throws IOException {
    final PantsProjectCache cache = PantsProjectCacheImpl.getInstance(myFixture.getProject());
    final VirtualFile root = getMainContentRoot();

    final AccessToken writeAccessToken = ApplicationManager.getApplication().acquireWriteActionLock(getClass());
    try {
      PsiTestUtil.addSourceRoot(myModule, VfsUtil.createDirectoryIfMissing(root, "bar"));
      PsiTestUtil.addSourceRoot(myModule, VfsUtil.createDirectoryIfMissing(root, "baz"));
      assertTrue(cache.folderContainsSourceRoot(VfsUtil.createDirectoryIfMissing(root, "baz")));
      assertFalse(cache.folderContainsSourceRoot(VfsUtil.createDirectoryIfMissing(root, "ba")));
      assertFalse(cache.folderContainsSourceRoot(VfsUtil.createDirectoryIfMissing(root, "bat")));
    }
    finally {
      writeAccessToken.finish();
    }
  }

  public void testFirstOne() throws IOException {
    final PantsProjectCache cache = PantsProjectCacheImpl.getInstance(myFixture.getProject());
    final VirtualFile root = getMainContentRoot();

    final AccessToken writeAccessToken = ApplicationManager.getApplication().acquireWriteActionLock(getClass());
    try {
      PsiTestUtil.addSourceRoot(myModule, VfsUtil.createDirectoryIfMissing(root, "abc"));
      PsiTestUtil.addSourceRoot(myModule, VfsUtil.createDirectoryIfMissing(root, "foo/bar"));
      PsiTestUtil.addSourceRoot(myModule, VfsUtil.createDirectoryIfMissing(root, "foo/baz"));
      assertTrue(cache.folderContainsSourceRoot(VfsUtil.createDirectoryIfMissing(root, "abc")));
      assertTrue(cache.folderContainsSourceRoot(VfsUtil.createDirectoryIfMissing(root, "foo")));
    }
    finally {
      writeAccessToken.finish();
    }
  }
}