// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PantsCompileCurrentTargetAction is a UI action that compiles target(s) related to the file under edit.
 */
public class PantsCompileCurrentTargetAction extends PantsCompileActionBase {

  protected PantsCompileCurrentTargetAction(String name) {
    super(name);
  }

  public PantsCompileCurrentTargetAction() {
    this("Compile target(s) in the selected editor");
  }

  /**
   * Find the target(s) that are only associated with the file opened in the selected editor.
   */
  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
    Set<Module> relatedModules =
      Arrays.stream(files)
        .map(x -> ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(x))
        .collect(Collectors.toSet());

    return relatedModules.stream()
      .map(PantsUtil::getNonGenTargetAddresses)
      .flatMap(Collection::stream);
  }
}
