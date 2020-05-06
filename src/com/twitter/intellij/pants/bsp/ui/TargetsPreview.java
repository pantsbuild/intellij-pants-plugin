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


  public void updatePreview (Set<PantsTargetAddress> addresses,
                             Function<PantsTargetAddress, CompletableFuture<Collection<PantsTargetAddress>>> expand)
    throws InterruptedException, ExecutionException {
    preview.setText("Loading...");
    this.addresses = addresses;
    List<CompletableFuture<Collection<PantsTargetAddress>>> futures =
      addresses.stream().map(expand::apply).collect(Collectors.toList());

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((value, error) -> {
      String newPreviewValue = futures.stream()
        .map(CompletableFuture::join)
        .flatMap(Collection::stream)
        .map(PantsTargetAddress::toAddressString)
        .sorted()
        .collect(Collectors.joining("\n")) + "\n";
      SwingUtilities.invokeLater(() -> {
        if(this.addresses == addresses) {
          preview.setText(newPreviewValue);
        }
      });
    });
  }
}
