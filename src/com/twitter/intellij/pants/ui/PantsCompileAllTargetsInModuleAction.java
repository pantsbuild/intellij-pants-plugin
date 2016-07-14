// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsUtil;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * PantsCompileAllTargetsInModuleAction is a UI action that is used to compile all Pants targets for a module
 */
public class PantsCompileAllTargetsInModuleAction extends AnAction {


  public PantsCompileAllTargetsInModuleAction() {
    super("Compile all targets in module");
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (project == null || file == null) {
      return;
    }

    Module module = ModuleUtil.findModuleForFile(file, project);
    Set<String> targetAddresses = PantsUtil.getTargetAddressesFromModule(module)
      .stream()
      .map(PantsTargetAddress::toString)
      .collect(Collectors.toSet());
    PantsMakeBeforeRun runner = (PantsMakeBeforeRun) ExternalSystemBeforeRunTaskProvider.getProvider(project, PantsMakeBeforeRun.ID);
    ApplicationManager.getApplication().executeOnPooledThread(() -> runner.executeTask(project, targetAddresses));
  }
}
