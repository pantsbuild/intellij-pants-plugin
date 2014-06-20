package com.twitter.intellij.pants.execution;


import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

public class PantsRunnerParameters implements Cloneable {
  protected String workingDir;
  protected String arguments;
  protected String executable;

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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PantsRunnerParameters that = (PantsRunnerParameters)o;
    if (arguments != null ? !arguments.equals(that.arguments) : that.arguments != null) return false;
    if (executable != null ? !executable.equals(that.executable) : that.executable != null) return false;
    if (workingDir != null ? !workingDir.equals(that.workingDir) : that.workingDir != null) return false;
    return true;
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
