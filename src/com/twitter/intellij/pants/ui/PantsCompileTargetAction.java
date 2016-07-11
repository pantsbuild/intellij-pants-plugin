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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PantsCompileTargetAction is a UI action that is used to compile a Pants target or collection of targets
 */
public class PantsCompileTargetAction extends AnAction {

  private HashSet<String> myTargetAddresses = new HashSet<String>();

  public PantsCompileTargetAction() {
    super("Compile all targets in module");
  }

  public PantsCompileTargetAction(String targetAddress) {
    super(targetAddress);
    myTargetAddresses.add(targetAddress);
  }

  public PantsCompileTargetAction(Collection<String> addresses) {
    this();
    myTargetAddresses.addAll(addresses);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (project == null || file == null) {
      return;
    }

    //  If empty constructor was called, make targets on the fly.
    if (myTargetAddresses.isEmpty()) {
      Module module = ModuleUtil.findModuleForFile(file, project);
      List<String> targetAddresses = PantsUtil.getTargetAddressesFromModule(module)
        .stream()
        .map(PantsTargetAddress::toString)
        .collect(Collectors.toList());
    }
    PantsMakeBeforeRun runner = (PantsMakeBeforeRun) ExternalSystemBeforeRunTaskProvider.getProvider(project, PantsMakeBeforeRun.ID);
    ApplicationManager.getApplication().executeOnPooledThread((Runnable)() -> {
      runner.executeTask(project, myTargetAddresses);
    });
  }
}
