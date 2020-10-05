// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

public class ExternalProjectUtil {

  public static void refresh(@NotNull Project project, ProjectSystemId id) {
    // This code needs to run on the dispatch thread, but in some cases
    // refreshAllProjects() is called on a non-dispatch thread; we use
    // invokeLater() to run in the dispatch thread.
    ApplicationManager.getApplication().invokeAndWait(
      () -> {
        ApplicationManager.getApplication().runWriteAction(() -> FileDocumentManager.getInstance().saveAllDocuments());

        final ImportSpecBuilder specBuilder = new ImportSpecBuilder(project, id);
        ProgressExecutionMode executionMode = ApplicationManager.getApplication().isUnitTestMode() ?
                                              ProgressExecutionMode.MODAL_SYNC : ProgressExecutionMode.IN_BACKGROUND_ASYNC;
        specBuilder.use(executionMode);
        ExternalSystemUtil.refreshProjects(specBuilder);
      });
  }
}
