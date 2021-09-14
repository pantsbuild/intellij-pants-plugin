// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.ui;


import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.twitter.intellij.pants.util.PantsUtil;

public class PantsOptionsInvalidationAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(AnActionEvent e) {
    PantsUtil.invalidatePluginCaches();
  }
}
