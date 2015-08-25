// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public interface PantsProjectCache extends ProjectComponent {
  boolean folderContainsSourceRoot(@NotNull VirtualFile file);
}
