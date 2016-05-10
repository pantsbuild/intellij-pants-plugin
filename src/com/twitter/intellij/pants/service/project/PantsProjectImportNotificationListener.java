// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.ide.actions.SynchronizeAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import org.jetbrains.annotations.NotNull;


public class PantsProjectImportNotificationListener extends ExternalSystemTaskNotificationListenerAdapter {
  @Override
  public void onSuccess(@NotNull ExternalSystemTaskId id) {
    super.onEnd(id);
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        // Explicitly synchronize the project after resolve because generated file can be changed.
        // Equivalent to File -> Synchronize.
        new SynchronizeAction().actionPerformed(null);
      }
    });
  }
}
