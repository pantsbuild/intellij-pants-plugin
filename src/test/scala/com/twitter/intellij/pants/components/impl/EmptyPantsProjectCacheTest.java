// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.twitter.intellij.pants.components.PantsProjectCache;

public class EmptyPantsProjectCacheTest extends BasePantsProjectCacheTest {

  public void testEmpty() {
    final PantsProjectCache cache = PantsProjectCache.getInstance(myFixture.getProject());
    assertFalse(cache.folderContainsSourceRoot(myFixture.getProject().getBaseDir()));
  }

}
