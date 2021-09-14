// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;

import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.util.Set;
import java.util.stream.Collectors;

public class TargetsPreview extends JPanel {
  private final JTextArea preview;

  public TargetsPreview() {
    preview = new JTextArea();
    preview.setAlignmentX(JTextArea.LEFT_ALIGNMENT);
    preview.setEditable(false);

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.add(preview);
  }

  public void setError() {
    preview.setText(PantsBundle.message("pants.bsp.invalid.targets.list"));
  }

  public void setLoading() {
    preview.setText(PantsBundle.message("pants.bsp.loading"));
  }

  public void updatePreview (Set<PantsTargetAddress> addresses) {
    String newText = addresses.stream()
        .map(PantsTargetAddress::toAddressString)
        .sorted()
        .collect(Collectors.joining("\n")) + "\n";
    preview.setText(newText);
  }
}
