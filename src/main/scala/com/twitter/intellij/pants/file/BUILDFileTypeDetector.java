// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.file;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.io.ByteSequence;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.python.PythonFileType;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BUILDFileTypeDetector implements FileTypeRegistry.FileTypeDetector {
  @Nullable
  @Override
  public FileType detect(@NotNull VirtualFile file, @NotNull ByteSequence firstBytes, @Nullable CharSequence firstCharsIfText) {
    return PantsUtil.isBUILDFileName(file.getName()) ? PythonFileType.INSTANCE : null;
  }

  @Override
  public int getVersion() {
    return 1;
  }
}
