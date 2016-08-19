// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class PantsOptions {
  /**
   * Cache of PantsOptions mapped from Pants executable files.
   */
  private static ConcurrentHashMap<File, PantsOptions> optionsCache = new ConcurrentHashMap<>();

  private Map<String, String> options;

  public static void clearCache() {
    optionsCache.clear();
  }

  public PantsOptions(final String rawOutput) {
    options = parseOptions(rawOutput);
  }

  public boolean has(String optionName) {
    return options.containsKey(optionName);
  }

  public Optional<String> get(String optionName) {
    return Optional.ofNullable(options.get(optionName));
  }

  public static Optional<PantsOptions> getPantsOptions(final Project myProject) {
    return PantsUtil.findPantsExecutable(myProject).map(file -> getPantsOptions(file.getPath()));
  }

  public boolean supportsManifestJar() {
    return has(PantsConstants.PANTS_OPTION_EXPORT_CLASSPATH_MANIFEST_JAR);
  }

  public boolean supportsAsyncCleanAll() {
    return has(PantsConstants.PANTS_OPTION_ASYNC_CLEAN_ALL);
  }

  public static PantsOptions getPantsOptions(@NotNull final String pantsExecutable) {
    File pantsExecutableFile = new File(pantsExecutable);
    PantsOptions cache = optionsCache.get(pantsExecutableFile);
    if (cache != null) {
      return cache;
    }

    GeneralCommandLine exportCommandline = PantsUtil.defaultCommandLine(pantsExecutable);
    exportCommandline.addParameters("options", PantsConstants.PANTS_CLI_OPTION_NO_COLORS);
    try {
      final ProcessOutput processOutput = PantsUtil.getProcessOutput(exportCommandline, null);
      PantsOptions result = new PantsOptions(processOutput.getStdout());
      optionsCache.put(pantsExecutableFile, result);
      return result;
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
