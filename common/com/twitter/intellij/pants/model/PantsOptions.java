// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;


public class PantsOptions {
  private Map<String, String> options;

  public PantsOptions(final String rawOutput) {
    options = parseOptions(rawOutput);
  }

  public boolean has(String optionName) {
    return options.containsKey(optionName);
  }

  @Nullable
  public String get(String optionName) {
    return options.get(optionName);
  }

  @Nullable
  public static PantsOptions getPantsOptions(final Project myProject) {
    VirtualFile pantsExecutable = PantsUtil.findPantsExecutable(myProject);
    if (pantsExecutable == null) {
      return null;
    }
    return getPantsOptions(pantsExecutable.getPath());
  }

  public boolean supportsManifestJar() {
    return has(PantsConstants.PANTS_OPTION_EXPORT_CLASSPATH_MANIFEST_JAR);
  }

  public static PantsOptions getPantsOptions(final String pantsExecutable) {
    final GeneralCommandLine exportCommandline = PantsUtil.defaultCommandLine(pantsExecutable);
    exportCommandline.addParameters("options", PantsConstants.PANTS_CLI_OPTION_NO_COLORS);
    try {
      final ProcessOutput processOutput = PantsUtil.getProcessOutput(exportCommandline, null);
      return new PantsOptions(processOutput.getStdout());
    }
    catch (ExecutionException e) {
      throw new PantsException("Failed:" + exportCommandline.getCommandLineString());
    }
  }

  // TODO https://github.com/pantsbuild/pants/issues/3161 to output options in json,
  // parsing will be simplified.
  private static Map<String, String> parseOptions(final String rawData) {
    String lines[] = rawData.split("\\r?\\n");

    Map<String, String> options = new HashMap<String, String>();
    for (String line : lines) {
      String fields[] = line.split(" = ", 2);
      if (fields.length != 2) {
        continue;
      }

      String optionValue = fields[1].replaceAll("\\s*\\(from (NONE|HARDCODED|CONFIG|ENVIRONMENT|FLAG).*", "");
      options.put(fields[0], optionValue);
    }

    return options;
  }
}
