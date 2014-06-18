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
import com.twitter.intellij.pants.util.Target;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.Nullable;


/**
  Created by ajohnson on 6/10/14.
 */
public abstract class PantsConfigurationProducerBase extends RunConfigurationProducer<PantsConfiguration> {

  final private String arguments;
  final private String goal;
  private String name;

  public PantsConfigurationProducerBase(String goal, String arguments) {
    super(PantsConfigurationType.getInstance().getFactory());
    this.arguments = arguments;
    this.goal = goal;
    this.name = goal;
  }

  @Nullable
  private PantsRunnerParameters getParametersFromContext(ConfigurationContext context) {
    final PyExpressionStatement statement = PsiTreeUtil.getParentOfType(context.getPsiLocation(), PyExpressionStatement.class);
    if (statement == null) {
      return null;
    }
    Target target = PantsPsiUtil.findTarget(statement);
    if (target == null) {
      return null;
    }
    name = goal + " " + target.getName();

    Location location = context.getLocation();
    if (location == null) {
      return null;
    }
    final PsiFile buildFile = location.getPsiElement().getContainingFile();
    if (!PantsUtil.BUILD.equals(buildFile.getName())) {
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

    String path = parent.getPath();
    int workingDirIndex = path.indexOf(context.getProject().getName());
    String workingDir = path.substring(0,workingDirIndex) + context.getProject().getName();
    String relativePath = path.substring(workingDirIndex + context.getProject().getName().length());
    //System.out.println("Workingdir: " + workingDir + ", relativePath: " + relativePath);
    //end new stuff
    params.setArguments(arguments + " " + parent.getPath() + ":" + target.getName());
    /*
    params.setArguments(relativePath.substring(1) + ":" + target.getName());
     */

    VirtualFile pantsExecutable = PantsUtil.findPantsExecutable(parent);
    if (pantsExecutable == null) {
      return null;
    }
    params.setExecutable(pantsExecutable.getPath());
    params.setWorkingDir(workingDir);
    return params;
  }

  @Override
  protected boolean setupConfigurationFromContext(PantsConfiguration configuration,ConfigurationContext context,Ref<PsiElement> sourceElement) {
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
    return configuration.getRunnerParameters().equals(params) && configuration.getName() == name;
  }
}
