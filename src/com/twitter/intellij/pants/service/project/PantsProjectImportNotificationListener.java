// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.metrics.PantsMetrics;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

/**
 * This class overrides the methods that will be called before/after project resolve
 * for metrics measurements and other procedures around project resolve.
 */
public class PantsProjectImportNotificationListener extends ExternalSystemTaskNotificationListenerAdapter {
  @Override
  public void onStart(@NotNull ExternalSystemTaskId id, String workingDir) {
    super.onStart(id, workingDir);
    if (id.findProject() == null) {
      return;
    }
    PantsMetrics.markResolveStart();
  }

  @Override
  public void onEnd(@NotNull ExternalSystemTaskId id) {
    Project project = id.findProject();
    if (project == null) {
      return;
    }
    PantsMetrics.markResolveEnd();
    PantsMetrics.prepareTimeIndexing(project);
    // Sync files as generated sources may have changed after `pants export` called
    // due to import and refresh.
    PantsUtil.synchronizeFiles();
    super.onEnd(id);
  }
}
