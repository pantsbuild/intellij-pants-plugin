// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PantsConfigurationContext {

  private final Module module;
  private final VirtualFile buildRoot;
  private final List<String> targets;
  private final PsiElement psiLocation;


  private PantsConfigurationContext(
    @NotNull Module myModule,
    @NotNull VirtualFile myBuildRoot,
    @NotNull List<String> myTargets,
    @NotNull PsiElement myPsiLocation
  ) {
    module = myModule;
    buildRoot = myBuildRoot;
    targets = myTargets;
    psiLocation = myPsiLocation;
  }

  @Nullable
  public static PantsConfigurationContext validatesAndCreate(ConfigurationContext context) {
    final Module module = context.getModule();
    if (module == null) return null;

    final VirtualFile buildRoot = PantsUtil.findBuildRoot(module);
    if (buildRoot == null) return null;

    final List<String> targets = PantsUtil.getNonGenTargetAddresses(module);
    if (targets.isEmpty()) return null;

    final PsiElement psiLocation = context.getPsiLocation();
    if (psiLocation == null) return null;

    return new PantsConfigurationContext(module, buildRoot, targets, psiLocation);
  }

  public Module getModule() {
    return module;
  }

  public VirtualFile getBuildRoot() {
    return buildRoot;
  }

  public List<String> getTargets() {
    return targets;
  }

  public PsiElement getLocation() {
    return psiLocation;
  }
}
