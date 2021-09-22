// Copyright 2018 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class CopyPathRelativeToBuildRootAction extends AnAction {
  @Override
  public void actionPerformed(AnActionEvent event) {
    Optional<VirtualFile> maybeBuildRoot = PantsUtil.findBuildRoot(event.getProject());
    Optional<VirtualFile> maybeFile = PantsUtil.getFileForEvent(event);
    if (!(maybeBuildRoot.isPresent() && maybeFile.isPresent())) {
      // Should be guarded by the check in update, so this should never happen.
      return;
    }
    VirtualFile buildRoot = maybeBuildRoot.get();
    VirtualFile file = maybeFile.get();
    Path filePath = Paths.get(file.getPath());
    Path buildRootPath = Paths.get(buildRoot.getPath());
    String relativePath = buildRootPath.relativize(filePath).toString();
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
      new StringSelection(relativePath),
      null
    );
  }

  @Override
  public void update(AnActionEvent event) {
    Optional<VirtualFile> maybeBuildRoot = PantsUtil.findBuildRoot(event.getProject());
    Optional<VirtualFile> maybeFile = PantsUtil.getFileForEvent(event);
    event.getPresentation().setVisible(maybeBuildRoot.isPresent() && maybeFile.isPresent());
  }
}
