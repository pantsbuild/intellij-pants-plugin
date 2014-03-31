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

public class PantsConfigurable extends SettingsEditor<PantsConfiguration> {
    JPanel myPanel;
    private JTextField myWorkingDirField;
    private JTextField myCommandLineField;
    private JTextField myPantsExeField;
    private JPanel myCommandLineExePanel;
    private JPanel myPantsExePanel;
    private JPanel myWorkingDirPanel;

    public PantsConfigurable(final Project project) {
    }

    public void applyEditorTo(@NotNull final PantsConfiguration configuration) throws ConfigurationException {
        final PantsRunnerParameters runnerParameters = configuration.getRunnerParameters();
        runnerParameters.setArguments(myCommandLineField.getText().trim());
        runnerParameters.setExecutable(myPantsExeField.getText().trim());
        runnerParameters.setWorkingDir(myWorkingDirField.getText().trim());
    }

    public void resetEditorFrom(final PantsConfiguration configuration) {
        final PantsRunnerParameters runnerParameters = configuration.getRunnerParameters();
        myWorkingDirField.setText(runnerParameters.getWorkingDir());
        myCommandLineField.setText(runnerParameters.getArguments());
        myPantsExeField.setText(runnerParameters.getExecutable());
    }

    @NotNull
    public JComponent createEditor() {
        return myPanel;
    }
}