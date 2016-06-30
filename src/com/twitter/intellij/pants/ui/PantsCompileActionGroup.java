// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PantsCompileActionGroup is an dynamic action group to compile Pants targets
 */
public class PantsCompileActionGroup extends ActionGroup {

  @NotNull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent event) {
    //  Deletes existing make and compile options.
    ActionManager actionManager = ActionManager.getInstance();
    DefaultActionGroup actionGroup = (DefaultActionGroup) actionManager.getAction("ProjectViewCompileGroup");
    actionGroup.remove(actionManager.getAction("MakeModule"));
    actionGroup.remove(actionManager.getAction("Compile"));

    final AnAction[] empty = new AnAction[0];

    if (event == null) return empty;

    Project project = event.getProject();
    VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);

    if (project == null || file == null) return empty;

    Module module = ModuleUtil.findModuleForFile(file, project);
    List<String> targetAddresses = PantsUtil.getTargetAddressesFromModule(module)
      .stream()
      .map(PantsTargetAddress::toString)
      .collect(Collectors.toList());

    if (targetAddresses.isEmpty()) return empty;

    LinkedList<AnAction> actions = new LinkedList<AnAction>();

    for (String targetAddress : targetAddresses) {
      actions.push(new PantsCompileTarget(targetAddress));
    }

    //  Adds compile all option for modules with multiple targets.
    if (targetAddresses.size() > 1) actions.addFirst(new PantsCompileTarget(targetAddresses));

    return actions.toArray(empty);
  }
}
