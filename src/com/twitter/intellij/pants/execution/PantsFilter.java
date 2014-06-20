package com.twitter.intellij.pants.execution;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * Created by ajohnson on 6/18/14.
 */

public class PantsFilter implements Filter {
  private final Project project;

  public PantsFilter(Project project) {
    this.project = project;
  }

  @Nullable
  @Override
  public Result applyFilter(String line, int entireLength) {
    int highlightBeginIndex = entireLength - line.length();
    int highlightEndIndex = highlightBeginIndex;
    if (line == null || line.length() == 0) {
      return null;
    }
    //shave off whitespace
    while ( line.length() > 0 && line.charAt(0) == ' ') {
      line = line.substring(1);
      highlightBeginIndex++;
      highlightEndIndex++;
    }
    line = line.replace("\n", "");
    if (line.length() == 0) {return null;}
    //detect url
    if (line.charAt(0) != '/') {
      return null;
    }
    String url = "";
    int i = 0;
    while (i < line.length() && line.charAt(i) != ' ' && line.charAt(i) != ':') {
      url += line.charAt(i);
      highlightEndIndex++;
      i++;
    }
    int lineNumber = 1;

    if (i < line.length() && line.charAt(i) == ':') {
      try {
        String number = line.substring(i+1, line.indexOf(":", i + 1));
        lineNumber = Integer.parseInt(number);
        highlightEndIndex += number.length() + 1;
      } catch (Exception e) {
      }
    }
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(url);
    if (file == null) {
      return null;
    }
    return new Result(highlightBeginIndex, highlightEndIndex, new OpenFileHyperlinkInfo(project, file, lineNumber-1));
  }
}
