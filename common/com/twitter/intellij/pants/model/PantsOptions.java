// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nullable;


public class PantsOptions {

  public static final String EXPORT_CLASSPATH_NAMING_STYLE = "export-classpath.use_old_naming_style";
  public static final String EXPORT_CLASSPATH_MANIFEST_JAR = "export-classpath.manifest_jar_only";
  public static final String JVM_DISTRIBUTIONS_PATHS = "--jvm-distributions-paths";
  public static final String NO_COLORS = "--no-colors";

  protected String rawData;

  public PantsOptions(final String pantsExecutable) {
    rawData = getPantsOptions(pantsExecutable);
  }

  protected PantsOptions(){

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
    return rawData.contains(EXPORT_CLASSPATH_NAMING_STYLE);
  }

  public boolean supportsManifestJar() {
    return rawData.contains(EXPORT_CLASSPATH_MANIFEST_JAR);
  }

  public static String getPantsOptions(final String pantsExecutable) {
    final GeneralCommandLine exportCommandline = PantsUtil.defaultCommandLine(pantsExecutable);
    exportCommandline.addParameters("options", NO_COLORS);
    try {
      final ProcessOutput processOutput = PantsUtil.getProcessOutput(exportCommandline, null);
      return processOutput.getStdout();
    }
    catch (ExecutionException e) {
      throw new PantsException("Failed:" + exportCommandline.getCommandLineString());
    }
  }
}
