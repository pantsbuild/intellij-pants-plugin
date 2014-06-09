/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * @author Jeka
 */
package com.twitter.intellij.pants.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class PantsConfiguration extends LocatableConfigurationBase {
  private PantsRunnerParameters myRunnerParameters = new PantsRunnerParameters();

  protected PantsConfiguration(Project project, ConfigurationFactory factory, String name) {
    super(project, factory, name);
  }

  public PantsRunnerParameters getRunnerParameters() {
    return myRunnerParameters;
  }

  @Override
  public void writeExternal(final Element element) throws WriteExternalException {
    super.writeExternal(element);
    XmlSerializer.serializeInto(myRunnerParameters, element);
  }

  @Override
  public void readExternal(final Element element) throws InvalidDataException {
    super.readExternal(element);
    XmlSerializer.deserializeInto(myRunnerParameters, element);
  }


  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    final String executable = myRunnerParameters.getExecutable();
    if (executable == null || VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(executable)) == null) {
      throw new ExecutionException("Can't find pants executable: " + executable);
    }
    final String workingDir = myRunnerParameters.getWorkingDir();
    if (workingDir == null || VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(workingDir)) == null) {
      throw new ExecutionException("Can't find workingDir: " + workingDir);
    }
    return new PantsRunningState(env, myRunnerParameters);
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    final String executable = myRunnerParameters.getExecutable();
    if (executable == null || VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(executable)) == null) {
      throw new RuntimeConfigurationException("Can't find pants executable: " + executable);
    }
    final String workingDir = myRunnerParameters.getWorkingDir();
    if (workingDir == null || VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(workingDir)) == null) {
      throw new RuntimeConfigurationException("Can't find workingDir: " + workingDir);
    }
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new PantsConfigurable(getProject());
  }

  @Override
  public RunConfiguration clone() {
    return new PantsConfiguration(getProject(), getFactory(), getName());
  }
}
