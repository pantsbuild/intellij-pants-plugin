// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.stream.Stream;

/**
 * PantsCompileCurrentTargetAction is a UI action that compiles target(s) related to the file under edit.
 */
public class PantsCompileCurrentTargetAction extends PantsCompileActionBase {

  public PantsCompileCurrentTargetAction() {
    super("Compile target(s) in the selected editor");
  }

  @NotNull
  @Override
  public Stream<String> getTargets(@NotNull AnActionEvent e, @NotNull Project project) {
    return PantsUtil.getFileInSelectedEditor(project)
      .flatMap(file -> PantsUtil.getModuleForFile(file, project))
      .map(PantsUtil::getNonGenTargetAddresses)
      .orElse(new LinkedList<>())
      .stream();
  }
}
