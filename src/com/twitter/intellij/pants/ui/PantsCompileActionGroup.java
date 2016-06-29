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

import java.util.Collection;

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

    if (event != null) {
      Project project = event.getProject();
      VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
      if (project != null && file != null) {
        Module module = ModuleUtil.findModuleForFile(file, project);
        Collection<PantsTargetAddress> targetAddresses = PantsUtil.getTargetAddressesFromModule(module);
        int numTargetAddresses = targetAddresses.size();

        if (numTargetAddresses >= 1) {
          int offset = numTargetAddresses > 1 ? 1 : 0;
          AnAction[] actions = new AnAction[numTargetAddresses + offset];

          if (numTargetAddresses > 1) actions[0] = new PantsCompileTarget(targetAddresses);

          int idx = offset;
          for (PantsTargetAddress targetAddress : targetAddresses) {
            actions[idx] = new PantsCompileTarget(targetAddress);
            idx++;
          }

          return actions;
        }
      }
    }
    return new AnAction[0];
  }
}
