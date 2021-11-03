// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PsiTestUtil;
import com.twitter.intellij.pants.components.PantsProjectCache;

import java.io.IOException;

public class FirstPantsProjectCacheTest extends BasePantsProjectCacheTest {

  public void testFirstOne() {
    final PantsProjectCache cache = PantsProjectCache.getInstance(myFixture.getProject());
    final VirtualFile root = getMainContentRoot();

    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        PsiTestUtil.addSourceRoot(getModule(), VfsUtil.createDirectoryIfMissing(root, "abc"));
        PsiTestUtil.addSourceRoot(getModule(), VfsUtil.createDirectoryIfMissing(root, "foo/bar"));
        PsiTestUtil.addSourceRoot(getModule(), VfsUtil.createDirectoryIfMissing(root, "foo/baz"));
        assertTrue(cache.folderContainsSourceRoot(VfsUtil.createDirectoryIfMissing(root, "abc")));
        assertTrue(cache.folderContainsSourceRoot(VfsUtil.createDirectoryIfMissing(root, "foo")));
      }
      catch (IOException e) {
        fail(e.getMessage());
      }
    });
  }
}
