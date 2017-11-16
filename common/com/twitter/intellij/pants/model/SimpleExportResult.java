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
import com.twitter.intellij.pants.util.TempFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This represents information from pants export that is not tied with targets,
 * obtained by running pants export command with no target arguments. Class
 * is in POJO form to facilitate json parsing.
 * <p>
 * See {@link com.twitter.intellij.pants.service.project.model.ProjectInfo} for
 * target level information from pants export.
 */
public class SimpleExportResult {

  /**
   * Cache of SimpleExportResult mapped from path of Pants executable files.
   */
  private static ConcurrentHashMap<File, SimpleExportResult> simpleExportCache = new ConcurrentHashMap<>();

  private static final Logger LOG = Logger.getInstance(SimpleExportResult.class);

  private String version;

  private Map<String, Map<String, String>> preferredJvmDistributions;

  private DefaultPlatform jvmPlatforms;

  public static void clearCache() {
    simpleExportCache.clear();
  }

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

  @NotNull
  public static SimpleExportResult getExportResult(@NotNull String pantsExecutable) throws PantsException {
    File pantsExecutableFile = new File(pantsExecutable);
    SimpleExportResult cache = simpleExportCache.get(pantsExecutableFile);
    if (cache != null) {
      return cache;
    }
    final GeneralCommandLine commandline = PantsUtil.defaultCommandLine(pantsExecutable);
    commandline.addParameters("export", PantsConstants.PANTS_CLI_OPTION_NO_COLORS);
    try (TempFile tempFile = TempFile.create("pants_export_run", ".out")) {
      commandline.addParameter(
        String.format("%s=%s", PantsConstants.PANTS_CLI_OPTION_EXPORT_OUTPUT_FILE,
                      tempFile.getFile().getPath()));
      final ProcessOutput processOutput = PantsUtil.getCmdOutput(commandline, null);
      if (!processOutput.checkSuccess(LOG)) {
        throw new PantsException(String.format(
          "Failed: command '%s' exited with non-zero status",
          commandline.getCommandLineString()));
      }
      SimpleExportResult result = parse(FileUtil.loadFile(tempFile.getFile()));
      simpleExportCache.put(pantsExecutableFile, result);
      return result;
    } catch (IOException | ExecutionException e) {
      throw new PantsException(String.format(
        "Failed: command '%s' with error \"%s\"",
        commandline.getCommandLineString(), e.toString()));
    }
    // unreachable
  }

  public Optional<String> getJdkHome(boolean strict) {
    final Optional<Map<String, String>> platformMap = Optional.ofNullable(
        getPreferredJvmDistributions().get(getJvmPlatforms().getDefaultPlatform()));
    final String exportKey = strict ? PantsConstants.PANTS_EXPORT_KEY_STRICT : PantsConstants.PANTS_EXPORT_KEY_NON_STRICT;
    return platformMap.flatMap(pmap -> Optional.ofNullable(pmap.get(exportKey)));
  }

  @VisibleForTesting
  @NotNull
  public static SimpleExportResult parse(@NotNull String output) {
    return GSON_PARSER.fromJson(output, SimpleExportResult.class);
  }
}
