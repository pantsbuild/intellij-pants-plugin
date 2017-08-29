// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Stream;

public class PantsGetCurrentFileTargets implements IPantsGetTargets {
  /**
   * Find the target(s) that are only associated with the file opened in the selected editor.
   */
  @NotNull
  @Override
  public Stream<String> apply(Optional<VirtualFile> vf, @NotNull Project project) {
    return Optional.ofNullable(FileEditorManager.getInstance(project).getSelectedTextEditor())
      .filter(editor -> editor instanceof EditorImpl)
      .map(editor -> ((EditorImpl) editor).getVirtualFile())
      .flatMap(file -> Optional.ofNullable(ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(file)))
      .map(PantsUtil::getNonGenTargetAddresses)
      .orElse(new LinkedList<>())
      .stream();
  }
}
