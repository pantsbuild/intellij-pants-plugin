// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.module.ModuleUtil;
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
    DefaultActionGroup actionGroup = (DefaultActionGroup) actionManager.getAction("ProjectViewCompileGroup");
    actionGroup.remove(actionManager.getAction("MakeModule"));
    actionGroup.remove(actionManager.getAction("Compile"));

    final AnAction[] emptyAction = new AnAction[0];

    List<AnAction> actions = Optional.ofNullable(event)
      // TODO: signal if no project found?
      .flatMap(ev -> PantsUtil.optJoin(Optional.ofNullable(ev.getProject()),
                                       IPantsGetTargets.getFileForEvent(ev)))
      .flatMap(projectFile -> Optional.ofNullable(
        ModuleUtil.findModuleForFile(projectFile.getSecond(), projectFile.getFirst())))
      .flatMap(module -> Optional.of(PantsUtil.getNonGenTargetAddresses(module))
        // TODO: signal if no addresses found?
        .filter(addrs -> (addrs.size() > 0))
        .map(addrs -> {
          List<AnAction> result = new LinkedList<>();
          if (addrs.size() > 1) {
            result.add(new PantsCompileAllTargetsInModuleAction(module));
          }
          addrs.forEach(target -> result.add(new PantsCompileTargetAction(target)));
          return result;
        })
      ).orElse(new LinkedList<>());

    return actions.toArray(emptyAction);
  }
}
