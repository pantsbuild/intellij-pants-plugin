// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.config;

import com.twitter.intellij.pants.PantsBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PantsProjectCompilerForm {
  private JPanel myMainPanel;
  private JComboBox myCompilerComboBox;
  private JTextPane myDescriptionTextPane;
  private JCheckBox myEnforceJdkCheckBox;

  private final CompilerValue myPantsCompiler =
    new CompilerValue(PantsBundle.message("pants.compile.pants.compiler"), PantsBundle.message("pants.compile.pants.compiler.description"));
  private final CompilerValue myIJCompiler =
    new CompilerValue(PantsBundle.message("pants.compile.intellij.compiler"), PantsBundle.message("pants.compile.intellij.compiler.description"));

  public PantsProjectCompilerForm() {
    myCompilerComboBox.addActionListener(
      new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          Object selectedItem = myCompilerComboBox.getSelectedItem();
          if (selectedItem instanceof CompilerValue) {
            myEnforceJdkCheckBox.setEnabled(((CompilerValue) selectedItem).getName().equals(myPantsCompiler.getName()));
          }
          myDescriptionTextPane.setText(
            selectedItem instanceof CompilerValue ? ((CompilerValue)selectedItem).getDescription() : ""
          );
        }
      }
    );
    myCompilerComboBox.addItem(myPantsCompiler);
    myCompilerComboBox.addItem(myIJCompiler);
  }

  public JPanel getMainPanel() {
    return myMainPanel;
  }

  public boolean isEnforceJdk() {
    return myEnforceJdkCheckBox.isSelected();
  }

  public void setEnforceJdk(boolean enforceJdk){
      myEnforceJdkCheckBox.setSelected(enforceJdk);
  }

  public JComboBox getCompilerComboBox() {
    return myCompilerComboBox;
  }

  public boolean isCompileWithIntellij() {
    return myCompilerComboBox.getSelectedItem() == myIJCompiler;
  }

  public void setCompileWithIntellij(boolean compileWithIntellij) {
    if (compileWithIntellij) {
      myCompilerComboBox.setSelectedItem(myIJCompiler);
      myEnforceJdkCheckBox.setEnabled(false);
    } else {
      myCompilerComboBox.setSelectedItem(myPantsCompiler);
      myEnforceJdkCheckBox.setEnabled(true);
    }
  }

  public static class CompilerValue {
    private String myName;
    private String myDescription;

    public CompilerValue(@NotNull @Nls String name, @NotNull @Nls String description) {
      myName = name;
      myDescription = description;
    }

    @NotNull @Nls
    public String getName() {
      return myName;
    }

    @NotNull @Nls
    public String getDescription() {
      return myDescription;
    }

    @Override
    public String toString() {
      return myName;
    }
  }
}
