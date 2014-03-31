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

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.ui.ConfigurationArgumentsHelpArea;
import com.intellij.execution.ui.ConfigurationModuleSelector;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;

public class PantsConfigurable extends SettingsEditor<PantsConfiguration> {
  JPanel myPanel;
  private JTextField myWorkingDirField;
  private JTextField myCommandLineField;
  private JTextField myPantsExeField;
  private JPanel myCommandLineExePanel;
  private JPanel myPantsExePanel;
  private JPanel myWorkingDirPanel;
  private ConfigurationArgumentsHelpArea myHelpArea;
  private LabeledComponent<JComboBox> myModule;
  //private final ConfigurationModuleSelector myModuleSelector;

  public PantsConfigurable(final Project project) {
    myHelpArea.setLabelText(ExecutionBundle.message("remote.configuration.remote.debugging.allows.you.to.connect.idea.to.a.running.jvm.label"));
    myHelpArea.setToolbarVisible();

//    final DocumentListener helpTextUpdater = new DocumentAdapter() {
//      public void textChanged(DocumentEvent event) {
//        updateHelpText();
//      }
//    };
////    myWorkingDirField.getDocument().addDocumentListener(helpTextUpdater);
////    myCommandLineField.getDocument().addDocumentListener(helpTextUpdater);
////    myPantsExeField.getDocument().addDocumentListener(helpTextUpdater);
////
////    final FocusListener fieldFocusListener = new FocusAdapter() {
////      public void focusLost(final FocusEvent e) {
////        updateHelpText();
////      }
////    };
//    myWorkingDirField.addFocusListener(fieldFocusListener);
//    myPantsExeField.addFocusListener(fieldFocusListener);

    //TODO ??
    //myModuleSelector = new ConfigurationModuleSelector(project, myModule.getComponent(), "<whole project>");
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
    //myModuleSelector.applyTo(configuration);
  }

  public void resetEditorFrom(final PantsConfiguration configuration) {
    myWorkingDirField.setText(configuration.WORKING_DIR);
    myCommandLineField.setText(configuration.COMMAND_LINE);
    myPantsExeField.setText(configuration.PANTS_EXE);
    //myModuleSelector.reset(configuration);
  }

  @NotNull
  public JComponent createEditor() {
    return myPanel;
  }

  private void createUIComponents() {
    // TODO: place custom component creation code here
  }

//  private void updateHelpText() {
//    boolean useSockets = !myRbShmem.isSelected();
//
//    final RemoteConnection connection = new RemoteConnection(
//      useSockets,
//      myHostName,
//      useSockets ? myPantsExeField.getText().trim() : myWorkingDirField.getText().trim(),
//      myRbListen.isSelected()
//    );
//    final String cmdLine = connection.getLaunchCommandLine();
//    // -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=7007
//    final String jvmtiCmdLine = cmdLine.replace("-Xdebug", "").replace("-Xrunjdwp:", "-agentlib:jdwp=").trim();
//    myHelpArea.updateText(jvmtiCmdLine);
//  }


}