// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsOutputMessage;
import org.jetbrains.annotations.Nullable;

public class PantsFilter implements Filter {
  private final Project project;

  public PantsFilter(Project project) {
    this.project = project;
  }

  @Nullable
  @Override
  public Result applyFilter(final String text, int entireLength) {
    PantsOutputMessage info = PantsOutputMessage.parseOutputMessage(text);
    if (info == null || ".".equals(info.getFilePath())) {
      return null;
    }
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(info.getFilePath());
    if (file == null) {
      return null;
    }
    final int start = entireLength - text.length() + info.getStart();
    final int end = entireLength - text.length() + info.getEnd();
    return new Result(start, end, new OpenFileHyperlinkInfo(project, file, info.getLineNumber()));
  }
}
