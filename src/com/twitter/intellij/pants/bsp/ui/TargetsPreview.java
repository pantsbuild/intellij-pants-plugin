// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;

import com.twitter.intellij.pants.bsp.PantsTargetAddress;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TargetsPreview extends JPanel {
  private final JTextArea preview;
  private Set<PantsTargetAddress> addresses;

  public TargetsPreview() {
    preview = new JTextArea();
    preview.setAlignmentX(JTextArea.LEFT_ALIGNMENT);
    preview.setEnabled(false);

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.add(preview);
  }

  public void setError() {
    preview.setText("Error");
  }

  public void setLoading() {
    preview.setText("Loading...");
  }

  public void updatePreview (Set<PantsTargetAddress> addresses)
     {
    preview.setText("Loading...");
    this.addresses = addresses;

    String newText =
      addresses
      .stream()
      .map(PantsTargetAddress::toAddressString)
      .sorted()
      .collect(Collectors.joining("\n")) + "\n";
    preview.setText(newText);

  }
}
