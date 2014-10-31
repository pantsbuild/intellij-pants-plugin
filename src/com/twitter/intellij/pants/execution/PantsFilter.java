// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by ajohnson on 6/18/14.
 */

public class PantsFilter implements Filter {
  private final Project project;

  public PantsFilter(Project project) {
    this.project = project;
  }

  @Nullable
  public static PantsFilterInfo parseLine(@NotNull String line) {
    int i = 0;
    if (line.contains("[error]") || line.contains("[warning]") || line.contains("[debug]")) {
      i = line.indexOf(']') + 1;
    }
    while (i < line.length() && (Character.isSpaceChar(line.charAt(i)) || line.charAt(i) == '\t')) {
      ++i;
    }
    final int start = i;
    while (i < line.length() && line.charAt(i) != ' ' && line.charAt(i) != '\n' && line.charAt(i) != ':') {
      ++i;
    }
    int end = i;
    i++;
    String filePath = line.substring(start, end);
    while (i < line.length() && Character.isDigit(line.charAt(i))) {
      ++i;
    }
    int lineNumber = 0;
    try {
      lineNumber = Integer.parseInt(line.substring(end + 1, i)) - 1;
      end = i;
    }
    catch (Exception e) {
    }
    return new PantsFilterInfo(start, end, filePath, lineNumber);
  }

  @Nullable
  @Override
  public Result applyFilter(final String text, int entireLength) {
    PantsFilterInfo info = parseLine(text);
    if (info == null || info.getFilePath() == ".") {
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

  public static class PantsFilterInfo {

    private final int start;
    private final int end;
    private final int lineNumber;
    private final String filePath;

    public PantsFilterInfo(int start, int end, String filePath, int lineNumber) {
      this.start = start;
      this.end = end;
      this.filePath = filePath;
      this.lineNumber = lineNumber;
    }

    public int getStart() {
      return start;
    }

    public int getEnd() {
      return end;
    }

    public int getLineNumber() {
      return lineNumber;
    }

    public String getFilePath() {
      return filePath;
    }
  }
}
