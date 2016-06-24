// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;

/**
 * PantsCompileAllTargets is a UI action that, when in a project, compiles all targets in the project
 */
public class PantsCompileTarget extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    System.out.println("got here boi");
  }

  //  TODO: compile all files in directory
  //  TODO: compile file
  //
  public void update(AnActionEvent e) {
    super.update(e);
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    boolean isEnabled = PantsUtil.findBuildRoot(file) != null;
    e.getPresentation().setEnabled(isEnabled);
  }
}
