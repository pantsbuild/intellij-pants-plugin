// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nullable;


public class PantsOptions {

  private String rawData;

  public PantsOptions(final String pantsExecutable) {
    rawData = getPantsOptions(pantsExecutable);
  }

  @Nullable
  public String getWorkdir() {
    String lines[] = rawData.split("\\r?\\n");
    String workdir = null;
    for (String line : lines) {
      /**
       * line looks like:
       * pants_workdir = /Users/abc/workspace/intellij-pants-plugin/.pants.d (from HARDCODED)
       */
      String start = "pants_workdir = ";
      String ending = " (from";
      if (line.contains(start)) {
        int beginIdx = line.indexOf(start) + start.length();
        int endIdx = line.indexOf(ending);
        if (beginIdx >= endIdx) {
          return null;
        }
        workdir = line.substring(beginIdx, endIdx);
        break;
      }
    }
    return workdir;
  }

  public boolean hasExportClassPathNamingStyle() {
    return rawData.contains(PantsConstants.PANTS_OPTION_EXPORT_CLASSPATH_NAMING_STYLE);
  }

  public static String getPantsOptions(final String pantsExecutable) {
    final GeneralCommandLine exportCommandline = PantsUtil.defaultCommandLine(pantsExecutable);
    exportCommandline.addParameters("options", PantsConstants.PANTS_OPTION_NO_COLORS);
    try {
      final ProcessOutput processOutput = PantsUtil.getProcessOutput(exportCommandline, null);
      return processOutput.getStdout();
    }
    catch (ExecutionException e) {
      throw new PantsException("Failed:" + exportCommandline.getCommandLineString());
    }
  }
}
