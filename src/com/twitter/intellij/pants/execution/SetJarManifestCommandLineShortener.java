// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.ConfigurationWithCommandLineShortener;
import com.intellij.execution.RunManagerListener;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.ShortenCommandLine;
import org.jetbrains.annotations.NotNull;


public class SetJarManifestCommandLineShortener implements RunManagerListener {
  @Override
  public void runConfigurationAdded(@NotNull RunnerAndConfigurationSettings settings) {
    if(settings.getConfiguration() instanceof ConfigurationWithCommandLineShortener) {
      ((ConfigurationWithCommandLineShortener)settings.getConfiguration()).setShortenCommandLine(ShortenCommandLine.MANIFEST);
    }
  }
}
