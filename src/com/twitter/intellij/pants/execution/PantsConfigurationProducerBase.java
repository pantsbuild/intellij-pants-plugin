// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyCallExpression;
import com.jetbrains.python.psi.PyExpressionStatement;
import com.twitter.intellij.pants.util.PantsPsiUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nullable;

public abstract class PantsConfigurationProducerBase extends RunConfigurationProducer<PantsConfiguration> {

  final private String arguments;
  final private String goal;
  private String name;

  public PantsConfigurationProducerBase(String goal) {
    super(PantsConfigurationType.getInstance().getFactory());
    this.arguments = goal;
    this.goal = goal;
    this.name = goal;
  }

  @Nullable
  private PantsRunnerParameters getParametersFromContext(ConfigurationContext context) {
    final PyExpressionStatement statement = PsiTreeUtil.getParentOfType(context.getPsiLocation(), PyExpressionStatement.class);
    if (statement == null) {
      return null;
    }
    final Pair<String, PyCallExpression> target = PantsPsiUtil.findTarget(statement);
    if (target == null) {
      return null;
    }
    final String targetName = target.getFirst();
    name = goal + " " + targetName;

    Location location = context.getLocation();
    if (location == null) {
      return null;
    }
    final PsiFile buildFile = location.getPsiElement().getContainingFile();
    if (!PantsUtil.isBUILDFileName(buildFile.getName())) {
      return null;
    }
    final VirtualFile virtualFile = buildFile.getVirtualFile();
    if (virtualFile == null) {
      return null;
    }
    VirtualFile parent = virtualFile.getParent();
    if (parent == null) {
      return null;
    }
    final PantsRunnerParameters params = new PantsRunnerParameters();

    VirtualFile pantsExecutable = PantsUtil.findPantsExecutable(parent);
    if (pantsExecutable == null) {
      return null;
    }
    String relativePath = VfsUtil.getRelativePath(parent, pantsExecutable.getParent(), '/');
    String workingDir = pantsExecutable.getParent().getPath();
    params.setArguments(arguments + " " + relativePath + ":" + targetName);
    params.setExecutable(pantsExecutable.getPath());
    params.setWorkingDir(workingDir);
    return params;
  }

  @Override
  protected boolean setupConfigurationFromContext(
    PantsConfiguration configuration,
    ConfigurationContext context,
    Ref<PsiElement> sourceElement
  ) {
    PantsRunnerParameters params = getParametersFromContext(context);
    if (params == null) {
      return false;
    }
    PantsRunnerParameters configParams = configuration.getRunnerParameters();
    configParams.setArguments(params.getArguments());
    configParams.setExecutable(params.getExecutable());
    configParams.setWorkingDir(params.getWorkingDir());
    configuration.setName(name);
    return true;
  }

  @Override
  public boolean isConfigurationFromContext(PantsConfiguration configuration, ConfigurationContext context) {
    PantsRunnerParameters params = getParametersFromContext(context);
    return configuration.getRunnerParameters().equals(params) && configuration.getName().equals(name);
  }
}
