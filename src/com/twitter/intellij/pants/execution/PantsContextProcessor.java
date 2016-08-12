// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PantsContextProcessor {

  public final Module module;
  public final VirtualFile buildRoot;
  public final List<String> targets;
  public final PsiElement psiLocation;


  private PantsContextProcessor(Module myModule, VirtualFile myBuildRoot, List<String> myTargets, PsiElement myPsiLocation) {
    module = myModule;
    buildRoot = myBuildRoot;
    targets = myTargets;
    psiLocation = myPsiLocation;
  }

  @Nullable
  public static PantsContextProcessor create(ConfigurationContext context) {
    final Module module = context.getModule();
    if (module == null) return null;

    final VirtualFile buildRoot = PantsUtil.findBuildRoot(module);
    if (buildRoot == null) return null;

    final List<String> targets = PantsUtil.getNonGenTargetAddresses(module);
    if (targets.isEmpty()) return null;

    final PsiElement psiLocation = context.getPsiLocation();
    if (psiLocation == null) return null;

    return new PantsContextProcessor(module, buildRoot, targets, psiLocation);
  }
}
