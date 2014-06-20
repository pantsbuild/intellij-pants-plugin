package com.twitter.intellij.pants.execution;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import net.sf.cglib.core.Local;
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
  @Override
  public Result applyFilter(final String text, int entireLength) {
    int highlightBeginIndex = entireLength - text.length();

    if (text == null) { return null;}
    String line = text.replace("\n", "").replace(" ", "");
    if (line.length() == 0) {return null;}
    if (line.charAt(0) != '/') {
      return null;
    }
    highlightBeginIndex += text.indexOf("/");
    int highlightEndIndex = highlightBeginIndex;
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
