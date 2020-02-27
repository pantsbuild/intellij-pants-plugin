// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.macro;

import com.intellij.ide.macro.Macro;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;

import java.util.Optional;

public class FilePathRelativeToBuiltRootMacro extends Macro {
  /**
   * @return corresponding name of this macro
   */
  @Override
  public String getName() {
    return "PantsFilePathRelativeToBuiltRoot";
  }

  @Override
  public String getDescription() {
    return "Relative path from build root";
  }

  @Override
  public String expand(final DataContext dataContext) {
    VirtualFile fileSelected = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
    if (fileSelected == null) {
      return null;
    }
    Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(fileSelected);
    if (!buildRoot.isPresent()) {
      return null;
    }
    return FileUtil.getRelativePath(VfsUtil.virtualToIoFile(buildRoot.get()), VfsUtil.virtualToIoFile(fileSelected));
  }
}
