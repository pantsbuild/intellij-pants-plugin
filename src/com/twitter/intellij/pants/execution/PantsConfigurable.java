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

import com.intellij.ide.util.TreeFileChooser;
import com.intellij.ide.util.TreeFileChooserFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.RawCommandLineEditor;
import com.twitter.intellij.pants.PantsBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PantsConfigurable extends SettingsEditor<PantsConfiguration> {
    JPanel myPanel;
    private TextFieldWithBrowseButton myExecutableField;
    private TextFieldWithBrowseButton myWorkingDirectoryField;
    private RawCommandLineEditor myArguments;

    public PantsConfigurable(final Project project) {
        myExecutableField.getButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                TreeFileChooser fileChooser = TreeFileChooserFactory.getInstance(project).createFileChooser(
                        PantsBundle.message("choose.pants.executable.file"),
                        null,
                        null,
                        new TreeFileChooser.PsiFileFilter() {
                            public boolean accept(PsiFile file) {
                                return "pants".equalsIgnoreCase(file.getName());
                            }
                        }
                );

                fileChooser.showDialog();

                PsiFile selectedFile = fileChooser.getSelectedFile();
                final VirtualFile virtualFile = selectedFile == null ? null : selectedFile.getVirtualFile();
                if (virtualFile != null) {
                    final String path = FileUtil.toSystemDependentName(virtualFile.getPath());
                    myExecutableField.setText(path);

                    if (StringUtil.isEmpty(myWorkingDirectoryField.getText())) {
                        final String folder = FileUtil.toSystemDependentName(virtualFile.getParent().getPath());
                        myWorkingDirectoryField.setText(folder);
                    }
                }
            }
        });
        myWorkingDirectoryField.getButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                TreeFileChooser fileChooser = TreeFileChooserFactory.getInstance(project).createFileChooser(
                        PantsBundle.message("choose.working.dir"),
                        null,
                        null,
                        new TreeFileChooser.PsiFileFilter() {
                            public boolean accept(PsiFile file) {
                                return file.isDirectory();
                            }
                        }
                );

                fileChooser.showDialog();

                PsiFile selectedFile = fileChooser.getSelectedFile();
                final VirtualFile virtualFile = selectedFile == null ? null : selectedFile.getVirtualFile();
                if (virtualFile != null) {
                    final String folder = FileUtil.toSystemDependentName(virtualFile.getPath());
                    myWorkingDirectoryField.setText(folder);
                }
            }
        });
    }

    public void applyEditorTo(@NotNull final PantsConfiguration configuration) throws ConfigurationException {
        final PantsRunnerParameters runnerParameters = configuration.getRunnerParameters();
        runnerParameters.setExecutable(myExecutableField.getText().trim());
        runnerParameters.setWorkingDir(myWorkingDirectoryField.getText().trim());
        runnerParameters.setArguments(myArguments.getText().trim());
    }

    public void resetEditorFrom(final PantsConfiguration configuration) {
        final PantsRunnerParameters runnerParameters = configuration.getRunnerParameters();
        myExecutableField.setText(runnerParameters.getExecutable());
        myWorkingDirectoryField.setText(runnerParameters.getWorkingDir());
        myArguments.setText(runnerParameters.getArguments());
    }

    @NotNull
    public JComponent createEditor() {
        return myPanel;
    }
}