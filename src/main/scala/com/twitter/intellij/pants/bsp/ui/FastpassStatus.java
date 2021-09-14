// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;

import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.AsyncProcessIcon;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class FastpassStatus extends JPanel {
  final JLabel myLabel;
  public FastpassStatus() {
    myLabel = new JLabel();
    myLabel.setText(" ");
    myLabel.setAlignmentX(LEFT_ALIGNMENT);
    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.add(myLabel);
  }

  public void setLoading(){
    this.removeAll();
    this.add(new AsyncProcessIcon( " "));
    this.updateUI();
  }

  public void setOk(){
    this.removeAll();
    this.add(myLabel);
    myLabel.setIcon(PlatformIcons.CHECK_ICON);
    myLabel.setText("");
    this.updateUI();
  }

  public void setWarning(String msg) {
    this.removeAll();
    this.add(myLabel);
    myLabel.setIcon(PlatformIcons.WARNING_INTRODUCTION_ICON);
    myLabel.setText(msg);
    this.updateUI();
  }

  public void setError(String msg) {
    this.removeAll();
    this.add(myLabel);
    myLabel.setIcon(PlatformIcons.ERROR_INTRODUCTION_ICON);
    myLabel.setText(msg);
    this.updateUI();
  }
}
