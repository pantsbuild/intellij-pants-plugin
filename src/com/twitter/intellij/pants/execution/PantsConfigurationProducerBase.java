package com.twitter.intellij.pants.execution;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyExpressionStatement;
import com.twitter.intellij.pants.util.PantsPsiUtil;
import com.twitter.intellij.pants.util.PantsUtil;


/**
  Created by ajohnson on 6/10/14.
 */
public abstract class PantsConfigurationProducerBase extends RunConfigurationProducer<PantsConfiguration> {

  final private String arguments;
  final private String name;

  public PantsConfigurationProducerBase() {
    super(PantsConfigurationType.getInstance().getFactory());
    this.arguments = null;
    this.name = PantsConfigurationType.getInstance().toString();
  }
  public PantsConfigurationProducerBase(String name, String arguments) {
    super(PantsConfigurationType.getInstance().getFactory());
    this.arguments = arguments;
    this.name = name;
  }

  private String executablePath(VirtualFile vFile){
    while (!(vFile.getName().equals("pants") && vFile.findChild("pants") != null)) {
      vFile = vFile.getParent();
    }
    return vFile.findChild("pants").getPath();
  }

  @Override
  protected boolean setupConfigurationFromContext(PantsConfiguration configuration,ConfigurationContext context,Ref<PsiElement> sourceElement) {
    final PyExpressionStatement statement = PsiTreeUtil.getParentOfType(context.getPsiLocation(), PyExpressionStatement.class);
    if (statement == null) {
      return false;
    }
    if (PantsPsiUtil.findTarget(statement) == null) {
      return false;
    }
    Location location = context.getLocation();
    if (location == null) {
      return false;
    }
    final PsiFile buildFile = location.getPsiElement().getContainingFile();
    if (!PantsUtil.BUILD.equals(buildFile.getName())) {
      return false;
    }
    final VirtualFile virtualFile = buildFile.getVirtualFile();
    if (virtualFile == null) {
      return false;
    }
    final VirtualFile parent = virtualFile.getParent();
    if (parent == null) {
      return false;
    }
    PantsRunnerParameters params = configuration.getRunnerParameters();
    params.setWorkingDir(parent.getPath());
    params.setArguments(arguments + parent.getPath());
    params.setExecutable(executablePath(parent));
    configuration.setName(name);
    return true;
  }

  @Override
  public boolean isConfigurationFromContext(PantsConfiguration configuration, ConfigurationContext context) {
    final PyExpressionStatement statement = PsiTreeUtil.getParentOfType(context.getPsiLocation(), PyExpressionStatement.class);
    if (statement == null) {
      return false;
    }
    if (PantsPsiUtil.findTarget(statement) == null) {
      return false;
    }
    Location location = context.getLocation();
    if (location == null) {
      return false;
    }
    final PsiFile buildFile = location.getPsiElement().getContainingFile();
    if (!PantsUtil.BUILD.equals(buildFile.getName())) {
      return false;
    }
    final VirtualFile virtualFile = buildFile.getVirtualFile();
    if (virtualFile == null) {
      return false;
    }
    PantsRunnerParameters params = configuration.getRunnerParameters();
    VirtualFile parent = virtualFile.getParent();
    if (parent == null) {
      return false;
    }
    if (params.getWorkingDir() == null || !params.getWorkingDir().equals(parent.getPath())) {
      return false;
    }
    if (params.getArguments() == null || !params.getArguments().equals(arguments)) { //adjust for different goals
      return false;
    }
    if (params.getExecutable() == null || !params.getExecutable().equals(executablePath(parent))) { //see relevant section of setup
      return false;
    }
    return true;
  }
}
