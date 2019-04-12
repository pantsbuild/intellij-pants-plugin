// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.updateSettings.impl.UpdateSettings;
import com.intellij.ui.components.JBCheckBox;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.execution.DefaultRunConfigurationSelector;
import org.jetbrains.annotations.NotNull;

public class PantsSystemSettingsControl implements ExternalSystemSettingsControl<PantsSettings> {
  private static final Logger LOG = Logger.getInstance(PantsSystemSettingsControl.class);

  private static final String UPDATE_URL = "https://raw.githubusercontent.com/pantsbuild/intellij-pants-plugin/master/pants-beta-updates.xml";

  private JBCheckBox myUpdateChannel;
  private ComboBox<DefaultRunConfigurationSelector.DefaultTestRunner> myTestRunner;
  private PantsSettings mySettings;

  public PantsSystemSettingsControl(PantsSettings settings) {
    mySettings = settings;
  }


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
    myTestRunner =  new ComboBox<>(
      DefaultRunConfigurationSelector.DefaultTestRunner.values(), 100);
    content.add(myUpdateChannel, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    content.add(myTestRunner, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
  }

  @Override
  public boolean isModified() {
    return myUpdateChannel.isSelected() != updaterContainsBetaChannel() ||
           myTestRunner.getSelectedItem() != mySettings.getDefaultTestRunner();
  }

  @Override
  public void reset() {
    myUpdateChannel.setSelected(updaterContainsBetaChannel());
    myTestRunner.setSelectedItem(mySettings.getDefaultTestRunner());
  }

  @Override
  public void apply(@NotNull PantsSettings settings) {
    if (myUpdateChannel.isSelected() && !updaterContainsBetaChannel()) {
      addBetaChannel();
    }
    if (!myUpdateChannel.isSelected() && updaterContainsBetaChannel()) {
      removeBetaChannel();
    }
    settings.setDefaultTestRunner((DefaultRunConfigurationSelector.DefaultTestRunner)myTestRunner.getSelectedItem());
    DefaultRunConfigurationSelector.DefaultTestRunner runner = settings.getDefaultTestRunner();
    DefaultRunConfigurationSelector.registerConfigs(runner);
  }

  @Override
  public boolean validate(@NotNull PantsSettings settings) throws ConfigurationException {
    if(settings.getDefaultTestRunner() == null) {
      return false;
    }
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
