// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp.ui;

import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.PlatformIcons;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.bsp.PantsTargetAddress;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Component;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FastpassImportedAddressesEditor extends JPanel {
  @NotNull final CheckBoxList<PantsTargetAddress> checkBoxList;
  @NotNull final JScrollPane checkboxListScroll;
  @NotNull final JCheckBox checkboxSelectAllFlat;
  @NotNull final JCheckBox checkboxSelectAllDeep;
  @NotNull final JLabel statusLabel;

  public FastpassImportedAddressesEditor(
    @NotNull Collection<PantsTargetAddress> availableTargetsInPath,
    @NotNull Set<PantsTargetAddress> allSelectedAddresses,
    @NotNull Path path,
    @NotNull Consumer<Collection<PantsTargetAddress>> update) {

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.setAlignmentX(Component.LEFT_ALIGNMENT);


    checkBoxList = createCheckboxList(availableTargetsInPath, allSelectedAddresses, update);
    checkboxListScroll = ScrollPaneFactory.createScrollPane(checkBoxList);

    checkboxSelectAllFlat = new JCheckBox(PantsBundle.message("pants.bsp.all.in.dir.flat"));

    checkboxSelectAllDeep = new JCheckBox(PantsBundle.message("pants.bsp.all.in.dir.recursive"));

    statusLabel = new JLabel("");

    if(blockedByParent(allSelectedAddresses, path)) {
      checkBoxList.setEnabled(false);
      checkboxSelectAllFlat.setEnabled(false);
      checkboxSelectAllDeep.setEnabled(false);
      statusLabel.setText(PantsBundle.message("pants.bsp.already.selected.by.parent"));
      statusLabel.setIcon(PlatformIcons.WARNING_INTRODUCTION_ICON);
    } else {
      checkboxSelectAllFlat.addItemListener(e -> handleSelectionChange(update, path));
      checkboxSelectAllDeep.addItemListener(e -> handleSelectionChange(update, path));
      statusLabel.setText(" ");
    }

    setupInitialCheckboxesSelection(allSelectedAddresses, path);

    this.add(checkboxListScroll);
    this.add(checkboxSelectAllFlat);
    this.add(checkboxSelectAllDeep);
    this.add(statusLabel);
  }

  private boolean blockedByParent(
    @NotNull Set<PantsTargetAddress> allSelectedAddresses,
    @NotNull Path path
  ) {
    return allSelectedAddresses.stream().anyMatch(x -> path.startsWith(x.getPath())
                                                       && !x.getPath().equals(path)
                                                       && x.getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP
    );
  }

  private void setupInitialCheckboxesSelection(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    if(flatAllInDirSelected(selected, path)) {
      checkboxSelectAllFlat.setSelected(true);
    } else if(deepAllInDirSelected(selected, path)) {
      checkboxSelectAllDeep.setSelected(true);
    } else if(singleTargetsSelected(selected, path)) {
      checkboxSelectAllDeep.setSelected(false);
      checkboxSelectAllFlat.setSelected(false);
    } else if(nothingSelected(selected, path)) {
      checkboxSelectAllDeep.setSelected(false);
      checkboxSelectAllFlat.setSelected(false);
    }
  }

  private boolean nothingSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream().noneMatch(x -> x.getPath().equals(path));
  }

  private boolean singleTargetsSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream()
      .filter(x -> x.getPath().equals(path))
      .allMatch(x -> x.getKind() == PantsTargetAddress.AddressKind.SINGLE_TARGET);
  }

  private boolean deepAllInDirSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream().anyMatch(x -> x.getPath().equals(path) && x.getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_DEEP);
  }

  private boolean flatAllInDirSelected(
    @NotNull Set<PantsTargetAddress> selected,
    @NotNull Path path
  ) {
    return selected.stream().anyMatch(x -> x.getPath().equals(path) && x.getKind() == PantsTargetAddress.AddressKind.ALL_TARGETS_FLAT);
  }

  private CheckBoxList<PantsTargetAddress> createCheckboxList(Collection<PantsTargetAddress> targets, Set<PantsTargetAddress> selected,
                                                              Consumer<Collection<PantsTargetAddress>> update) {
    CheckBoxList<PantsTargetAddress> checkboxPanel =  new CheckBoxList<>();
    checkboxPanel.setCheckBoxListListener ((index, value) -> {
      List<PantsTargetAddress> newSelected = IntStream
        .range(0, checkboxPanel.getItemsCount())
        .filter(checkboxPanel::isItemSelected)
        .mapToObj(checkboxPanel::getItemAt)
        .collect(Collectors.toList());
      update.accept(newSelected);
    });

    checkboxPanel.setItems(new ArrayList<>(targets), PantsTargetAddress::toAddressString);
    for (PantsTargetAddress target : targets) {
      checkboxPanel.setItemSelected(target, selected.contains(target));
    }
    return checkboxPanel;
  }

  void handleSelectionChange(Consumer<Collection<PantsTargetAddress>> update, Path path) {
    if(checkboxSelectAllDeep.isSelected()) {
      update.accept(Collections.singletonList(PantsTargetAddress.allTargetsInDirDeep(path)));

      checkBoxList.setEnabled(false);
      checkboxSelectAllFlat.setEnabled(false);
      checkboxSelectAllDeep.setEnabled(true);
    } else if(checkboxSelectAllFlat.isSelected()){
      update.accept(Collections.singletonList(PantsTargetAddress.allTargetsInDirFlat(path)));

      checkBoxList.setEnabled(false);
      checkboxSelectAllFlat.setEnabled(true);
      checkboxSelectAllDeep.setEnabled(false);
    } else {
      update.accept(Collections.emptyList());

      checkBoxList.setEnabled(true);
      checkboxSelectAllDeep.setEnabled(true);
      checkboxSelectAllFlat.setEnabled(true);
    }
  };

}
