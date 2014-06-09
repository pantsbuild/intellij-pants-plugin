package com.twitter.intellij.pants.execution;


import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

public class PantsRunnerParameters implements Cloneable {
  public String workingDir;
  public String arguments;
  public String executable;

  @Nullable
  public String getWorkingDir() {
    return workingDir;
  }

  public void setWorkingDir(String workingDir) {
    this.workingDir = StringUtil.nullize(workingDir);
  }

  @Nullable
  public String getArguments() {
    return arguments;
  }

  public void setArguments(String arguments) {
    this.arguments = StringUtil.nullize(arguments);
  }

  @Nullable
  public String getExecutable() {
    return executable;
  }

  public void setExecutable(String executable) {
    this.executable = StringUtil.nullize(executable);
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    final PantsRunnerParameters result = new PantsRunnerParameters();
    result.setArguments(getArguments());
    result.setExecutable(getExecutable());
    result.setWorkingDir(getWorkingDir());
    return result;
  }
}
