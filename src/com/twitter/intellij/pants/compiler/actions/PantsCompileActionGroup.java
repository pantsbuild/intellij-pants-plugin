// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * PantsCompileActionGroup is an dynamic action group to compile Pants targets
 */
public class PantsCompileActionGroup extends ActionGroup {

  @NotNull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent event) {
    //  Deletes existing make and compile options.
    ActionManager actionManager = ActionManager.getInstance();

    // TODO: don't remove these actions or put on our own unless we're in a
    // pants project, so we don't clobber these actions in a non-pants project
    DefaultActionGroup actionGroup = (DefaultActionGroup) actionManager.getAction(PantsConstants.ACTION_COMPILE_GROUP_ID);
    actionGroup.remove(actionManager.getAction(IdeActions.ACTION_MAKE_MODULE));
    actionGroup.remove(actionManager.getAction(IdeActions.ACTION_COMPILE));

    final AnAction[] emptyAction = new AnAction[0];

    if (event == null) {
      return emptyAction;
    }
    Project project = event.getProject();
    Optional<VirtualFile> eventFile = PantsUtil.getFileForEvent(event);
    // TODO: signal if no project found?
    if (project == null || !eventFile.isPresent()) {
      return emptyAction;
    }
    VirtualFile file = eventFile.get();

    List<AnAction> actions = new LinkedList<>();

    Module module = ModuleUtil.findModuleForFile(file, project);
    if (module == null) {
      return emptyAction;
    }
    List<String> targetAddresses = PantsUtil.getNonGenTargetAddresses(module);
    // TODO: signal if no addresses found?
    if (targetAddresses.isEmpty()) {
      return emptyAction;
    }

    actions.add(new PantsLintTargetAction(targetAddresses));
    // Adds compile all option for modules with multiple targets.
    if (targetAddresses.size() > 1) {
      actions.add(new PantsCompileAllTargetsInModuleAction(Optional.of(module)));
    }
    targetAddresses.forEach(target -> actions.add(new PantsCompileTargetAction(target)));

    return actions.toArray(emptyAction);
  }
}
