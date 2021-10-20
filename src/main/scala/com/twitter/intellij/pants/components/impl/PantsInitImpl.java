// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.util.registry.Registry;
import com.twitter.intellij.pants.metrics.PantsMetrics;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PantsInitImpl implements AppLifecycleListener {

  public static void initialize() {
    final String key = PantsConstants.SYSTEM_ID.getId() + ExternalSystemConstants.USE_IN_PROCESS_COMMUNICATION_REGISTRY_KEY_SUFFIX;
    Registry.get(key).setValue(true);
  }

  @Override
  public void appFrameCreated(@NotNull List<String> commandLineArgs) {
    initialize();
  }

  @Override
  public void appWillBeClosed(boolean isRestart) {
    PantsUtil.scheduledThreadPool.shutdown();
    PantsMetrics.globalCleanup();
  }
}
