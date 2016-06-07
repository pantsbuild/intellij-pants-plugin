// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * This class overrides the methods that will be called before/after project import/refresh.
 */
public class PantsProjectImportNotificationListener extends ExternalSystemTaskNotificationListenerAdapter {
  @Override
  public void onEnd(@NotNull ExternalSystemTaskId id) {
    super.onEnd(id);
    // Sync files as generated sources may have changed after `pants export` called
    // due to import and refresh.
    PantsUtil.synchronizeFiles();
  }
}
