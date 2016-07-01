// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;

import java.util.Collection;
import java.util.HashSet;

/**
 * PantsCompileTargetAction is a UI action that is used to compile a Pants target or collection of targets
 */
public class PantsCompileTargetAction extends AnAction {

  private HashSet<String> myTargetAddresses = new HashSet<String>();

  public PantsCompileTargetAction(String targetAddress) {
    super(targetAddress);
    myTargetAddresses.add(targetAddress);
  }

  public PantsCompileTargetAction(Collection<String> addresses) {
    super("Compile all targets in module");
    myTargetAddresses.addAll(addresses);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    PantsMakeBeforeRun runner = (PantsMakeBeforeRun) ExternalSystemBeforeRunTaskProvider.getProvider(project, PantsMakeBeforeRun.ID);
    ApplicationManager.getApplication().executeOnPooledThread((Runnable)() -> {
      runner.executeTask(project, myTargetAddresses);
    });
  }
}
