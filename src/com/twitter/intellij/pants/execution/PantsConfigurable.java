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
 * Class RemoteConfigurable
 * @author Jeka
 */
package com.twitter.intellij.pants.execution;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;

public class PantsConfigurable extends SettingsEditor<PantsConfiguration> {
  JPanel myPanel;
  private JTextField myWorkingDirField;
  private JTextField myCommandLineField;
  private JTextField myPantsExeField;
  private JPanel myCommandLineExePanel;
  private JPanel myPantsExePanel;
  private JPanel myWorkingDirPanel;

  public PantsConfigurable(final Project project) {
    // making educated guesses to prepopulate the fields with
    File workingDir = guessWorkingDir(new File(project.getBasePath()).getAbsoluteFile());

    myWorkingDirField.setText(workingDir.toString());
    myPantsExeField.setText(new File(workingDir, "pants").toString());
    myCommandLineField.setText("goal compile <put_your_target_here>");
  }

  private File guessWorkingDir(File startDir) {
    // making the assumption that .pants.d is located in the same place as the working dir
    File basePath = startDir;
    while (basePath != null) {
      if (basePath.getParentFile() != null && basePath.getName().equals(".pants.d")) {
        return basePath.getParentFile();
      }
      basePath = basePath.getParentFile();
    }
    // no luck, return what we got
    return startDir;
  }

  public void applyEditorTo(@NotNull final PantsConfiguration configuration) throws ConfigurationException {
    configuration.COMMAND_LINE = myCommandLineField.getText().trim();
    if (configuration.COMMAND_LINE != null && configuration.COMMAND_LINE.isEmpty()) {
      configuration.COMMAND_LINE = null;
    }
    configuration.PANTS_EXE = myPantsExeField.getText().trim();
    if (configuration.PANTS_EXE != null && configuration.PANTS_EXE.isEmpty()) {
      configuration.PANTS_EXE = null;
    }
    configuration.WORKING_DIR = myWorkingDirField.getText().trim();
    if (configuration.WORKING_DIR != null && configuration.WORKING_DIR.isEmpty()) {
      configuration.WORKING_DIR = null;
    }
  }

  public void resetEditorFrom(final PantsConfiguration configuration) {
    if (configuration.WORKING_DIR != null) {
      myWorkingDirField.setText(configuration.WORKING_DIR);
    }
    if (configuration.COMMAND_LINE != null) {
      myCommandLineField.setText(configuration.COMMAND_LINE);
    }
    if (configuration.PANTS_EXE != null) {
      myPantsExeField.setText(configuration.PANTS_EXE);
    }
  }

  @NotNull
  public JComponent createEditor() {
    return myPanel;
  }

  private void createUIComponents() {
    // TODO: place custom component creation code here
  }
}