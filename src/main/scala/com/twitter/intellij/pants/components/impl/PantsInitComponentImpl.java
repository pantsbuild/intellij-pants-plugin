// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.util.registry.Registry;
import com.twitter.intellij.pants.components.PantsInitComponent;
import com.twitter.intellij.pants.metrics.PantsMetrics;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

public class PantsInitComponentImpl implements PantsInitComponent {
  @NotNull
  @Override
  public String getComponentName() {
    return "pants.init";
  }

  public PantsInitComponentImpl() {
    // The Pants plugin doesn't do so many computations for building a project
    // to start an external JVM each time.
    // The plugin only calls `export` goal and parses JSON response.
    // So it will be in process all the time.
    final String key = PantsConstants.SYSTEM_ID.getId() + ExternalSystemConstants.USE_IN_PROCESS_COMMUNICATION_REGISTRY_KEY_SUFFIX;
    Registry.get(key).setValue(true);
  }

  @Override
  public void dispose() {
    PantsUtil.scheduledThreadPool.shutdown();
    PantsMetrics.globalCleanup();
  }
}
