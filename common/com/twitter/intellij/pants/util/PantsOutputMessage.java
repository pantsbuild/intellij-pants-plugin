// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

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
   * @param line                 the output
   * @param onlyCompilerMessages will look only for compiler specific message e.g. with a log level
   * @param checkFileExistence   will check if the parsed {@code myFilePath} exists
   */
  @Nullable
  public static PantsOutputMessage parseMessage(@NotNull String line, boolean onlyCompilerMessages, boolean checkFileExistence) {
    int i = 0;
    final boolean isError = isError(line);
    final boolean isWarning = isWarning(line);
    if (isError || isWarning || line.contains("[debug]")) {
      i = line.indexOf(']') + 1;
    }
    else if (onlyCompilerMessages) {
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

  public static boolean isWarning(@NotNull String line) {
    return containsLevel(line, "warning") || containsLevel(line, "warn");
  }

  public static boolean isError(@NotNull String line) {
    return containsLevel(line, "error");
  }

  public static boolean containsLevel(@NotNull String line, @NotNull String level) {
    return StringUtil.contains(line, "[" + level + "]") ||
           StringUtil.contains(line, " " + level + ":");
  }

  public enum Level {
    ERROR, WARNING, INFO
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PantsOutputMessage message = (PantsOutputMessage) o;

    if (myEnd != message.myEnd) return false;
    if (myLineNumber != message.myLineNumber) return false;
    if (myStart != message.myStart) return false;
    if (myFilePath != null ? !myFilePath.equals(message.myFilePath) : message.myFilePath != null) return false;
    if (myLevel != message.myLevel) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      myStart,
      myEnd,
      myLineNumber,
      myFilePath,
      myLevel
    );
  }
}
