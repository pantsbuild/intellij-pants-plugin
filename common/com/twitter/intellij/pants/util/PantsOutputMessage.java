// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class PantsOutputMessage {
  private final int myStart;
  private final int myEnd;
  private final int myLineNumber;
  private final String myFilePath;
  private final Level myLevel;

  public PantsOutputMessage(int start, int end, String filePath, int lineNumber) {
    this(start, end, filePath, lineNumber, Level.INFO);
  }

  public PantsOutputMessage(int start, int end, String filePath, int lineNumber, Level level) {
    myStart = start;
    myEnd = end;
    myFilePath = filePath;
    myLineNumber = lineNumber;
    myLevel = level;
  }

  public int getStart() {
    return myStart;
  }

  public int getEnd() {
    return myEnd;
  }

  public int getLineNumber() {
    return myLineNumber;
  }

  @NotNull
  public String getFilePath() {
    return myFilePath;
  }

  @NotNull
  public Level getLevel() {
    return myLevel;
  }

  @Override
  public String toString() {
    return "PantsOutputMessage{" +
           "start=" + myStart +
           ", end=" + myEnd +
           ", lineNumber=" + myLineNumber +
           ", filePath='" + myFilePath + '\'' +
           '}';
  }

  @Nullable
  public static PantsOutputMessage parseOutputMessage(@NotNull String message) {
    return parseMessage(message, false, false);
  }

  @Nullable
  public static PantsOutputMessage parseCompilerMessage(@NotNull String message) {
    return parseMessage(message, true, true);
  }

  /**
   * @param line of an output
   * @param onlyCompilerMessages will look only for compiler specific message e.g. with a log level
   * @param checkFileExistence will check if the parsed {@code myFilePath} exists
   */
  @Nullable
  public static PantsOutputMessage parseMessage(@NotNull String line, boolean onlyCompilerMessages, boolean checkFileExistence) {
    int i = 0;
    final boolean isError = line.contains("[error]");
    final boolean isWarning = line.contains("[warning]") || line.contains("[warn]");
    if (isError || isWarning || line.contains("[debug]")) {
      i = line.indexOf(']') + 1;
    } else if (onlyCompilerMessages) {
      return null;
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
    final String filePath = line.substring(start, end);
    if (checkFileExistence && !(new File(filePath).exists())) {
      return null;
    }
    while (i < line.length() && Character.isDigit(line.charAt(i))) {
      ++i;
    }
    int lineNumber = 0;
    try {
      lineNumber = Integer.parseInt(line.substring(end + 1, i)) - 1;
      end = i;
    }
    catch (Exception ignored) {
    }
    final Level level = isError ? Level.ERROR : isWarning ? Level.WARNING : Level.INFO;
    return new PantsOutputMessage(start, end, filePath, lineNumber, level);
  }

  public static enum Level {
    ERROR, WARNING, INFO
  }
}
