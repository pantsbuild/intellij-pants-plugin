// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PantsIncrementalImportManager extends AnAction {


  // Map from Project Id to Pants export result.
  private static ConcurrentMap<String, String> pantsExportResultMap = new ConcurrentHashMap<>();
  //public static File lastExportFile;

  @Nullable
  public static String getPantsExportResult(String projectId) {
    return pantsExportResultMap.get(projectId);
  }

  public static void addPantsExportResult(String projectId, String pantsExportResult) {
    pantsExportResultMap.put(projectId, pantsExportResult);
  }

  public static void clearCache() {
    pantsExportResultMap.clear();
  }


  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    PantsUtil.refreshAllProjects(project);
  }
}
