// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import com.intellij.ui.components.JBCheckBox;
import com.twitter.intellij.pants.PantsBundle;
import org.jetbrains.annotations.NotNull;

public class PantsSystemSettingsControl implements ExternalSystemSettingsControl<PantsSettings> {
  private static final Logger LOG = Logger.getInstance(PantsSystemSettingsControl.class);

  private static final String UPDATE_URL = "https://raw.githubusercontent.com/pantsbuild/intellij-pants-plugin/master/pants-beta-updates.xml";

  private JBCheckBox myUpdateChannel;

  public boolean updaterContainsBetaChannel() {
    final UpdateSettings updateSettings = UpdateSettings.getInstance();
    return updateSettings.getStoredPluginHosts().contains(UPDATE_URL);
  }

  public void addBetaChannel() {
    LOG.info("Enabled BETA update channel for Pants Plugin!");
    final UpdateSettings updateSettings = UpdateSettings.getInstance();
    updateSettings.getStoredPluginHosts().add(UPDATE_URL);
    UpdateChecker.updateAndShowResult();
  }

  public void removeBetaChannel() {
    LOG.info("Disabled BETA update channel for Pants Plugin!");
    final UpdateSettings updateSettings = UpdateSettings.getInstance();
    updateSettings.getStoredPluginHosts().remove(UPDATE_URL);
  }

  @Override
  public void fillUi(@NotNull PaintAwarePanel content, int indentLevel) {
    myUpdateChannel = new JBCheckBox(PantsBundle.message("pants.settings.text.update.channel"));
    content.add(myUpdateChannel, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
  }

  @Override
  public boolean isModified() {
    return myUpdateChannel.isSelected() != updaterContainsBetaChannel();
  }

  @Override
  public void reset() {
    myUpdateChannel.setSelected(updaterContainsBetaChannel());
  }

  @Override
  public void apply(@NotNull PantsSettings settings) {
    if (myUpdateChannel.isSelected() && !updaterContainsBetaChannel()) {
      addBetaChannel();
    }
    if (!myUpdateChannel.isSelected() && updaterContainsBetaChannel()) {
      removeBetaChannel();
    }
  }

  @Override
  public boolean validate(@NotNull PantsSettings settings) throws ConfigurationException {
    return true;
  }

  public void disposeUIResources() {
    ExternalSystemUiUtil.disposeUi(this);
  }

  @Override
  public void showUi(boolean show) {
    ExternalSystemUiUtil.showUi(this, show);
  }
}
