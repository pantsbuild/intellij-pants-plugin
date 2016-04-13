// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * This represents information from pants export not tied with targets, which is
 * obtained by running pants export command with no target arguments.
 *
 * {@link com.twitter.intellij.pants.service.project.model.ProjectInfo} contains
 * Target level information.
 */
public class SimpleExportResult {
  private static final Logger LOG = Logger.getInstance(SimpleExportResult.class);

  private String version;

  private Map<String, Map<String, String>> preferredJvmDistributions;

  private DefaultPlatform jvmPlatforms;

  public Map<String, Map<String, String>> getPreferredJvmDistributions() {
    return preferredJvmDistributions;
  }

  public DefaultPlatform getJvmPlatforms() {
    return jvmPlatforms;
  }

  public String getVersion() {
    return version;
  }

  public static class DefaultPlatform {
    private String defaultPlatform;

    public String getDefaultPlatform() {
      return defaultPlatform;
    }
  }

  private static final Gson GSON_PARSER = new GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

  public static SimpleExportResult getExportResult(String pantsExecutable) {
    final GeneralCommandLine commandline = PantsUtil.defaultCommandLine(pantsExecutable);
    commandline.addParameters("export", PantsConstants.PANTS_OPTION_NO_COLORS);
    try {
      File outputFile = File.createTempFile("pants_export_run", ".out");
      commandline.addParameter(
        String.format("%s=%s", PantsConstants.PANTS_OPTION_EXPORT_OUTPUT_FILE, outputFile.getPath()));
      final ProcessOutput processOutput = PantsUtil.getProcessOutput(commandline, null);
      if (processOutput.checkSuccess(LOG)) {
        return parse(FileUtil.loadFile(outputFile));
      }
    }
    catch (IOException | ExecutionException e) {
      // Fall-through to handle outside the block.
    }
    throw new PantsException("Failed:" + commandline.getCommandLineString());
  }

  @VisibleForTesting
  public static SimpleExportResult parse(String output) {
    return GSON_PARSER.fromJson(output, SimpleExportResult.class);
  }
}
