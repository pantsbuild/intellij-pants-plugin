// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTaskProvider;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.model.PantsTargetAddress;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PantsCompileTarget is a UI action that is used to compile a Pants target or collection or targets
 */
public class PantsCompileTarget extends AnAction {

  HashSet<String> myTargetAddresses = new HashSet<String>();

  public PantsCompileTarget() { super(); }

  public PantsCompileTarget(PantsTargetAddress targetAddress) {
    super("Compile " + targetAddress.getTargetName());
    this.myTargetAddresses .add(targetAddress.toString());
  }

  public PantsCompileTarget(Collection<PantsTargetAddress> addresses) {
    super("Compile all targets in module");
    Set<String> paths = addresses
      .stream()
      .map(PantsTargetAddress::toString)
      .collect(Collectors.toSet());
    this.myTargetAddresses.addAll(paths);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    PantsMakeBeforeRun runner = (PantsMakeBeforeRun) ExternalSystemBeforeRunTaskProvider.getProvider(project, PantsMakeBeforeRun.ID);
    runner.executeTask(project, myTargetAddresses);
  }
}
