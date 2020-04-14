// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;


import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

class FastpassAddressesViewPanel extends JComponent {
  public FastpassAddressesViewPanel() {
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.add(mainPanel);
  }

  private JPanel createMainPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    this.setPreferredSize(JBUI.size(300, 500));
    return mainPanel;
  }

  @NotNull
  JPanel mainPanel = createMainPanel();

  private void updateTargetsListWithMessage(JComponent icon){
    mainPanel.removeAll();
    mainPanel.add(icon);
  }

  public void  setItems(Collection<PantsTargetAddress> value,
                        Set<PantsTargetAddress> selected,
                        Path path,
                        Consumer<Collection<PantsTargetAddress>> update
  ) {
    mainPanel.removeAll();
    mainPanel.add(new FastpassImportedAddressesEditor(value, selected, path, update));
  }


  public void setLoading() {
    updateTargetsListWithMessage(new AsyncProcessIcon(""));
  }

  public void clear() {
    mainPanel.removeAll();
  }

  public void fetchTargetsError() {
    mainPanel.removeAll();

    JLabel errorLabel = new JLabel(
      PantsBundle.message("pants.bsp.error.failed.to.fetch.targets.in.dir"),
      PlatformIcons.ERROR_INTRODUCTION_ICON,
      SwingConstants.CENTER
    );
    mainPanel.add(errorLabel);
  }
}