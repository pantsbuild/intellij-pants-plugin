// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.JUnitPatcher;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.Nullable;


public class PantsJUnitPatcher extends JUnitPatcher {
  /**
   * @deprecated override {@link #patchJavaParameters(Module, JavaParameters)} instead
   */
  @Override
  public void patchJavaParameters(JavaParameters javaParameters) {
    int x = 5;
  }

  @Override
  public void patchJavaParameters(@Nullable Module module, JavaParameters javaParameters) {
    patchJavaParameters(javaParameters);
  }
}
