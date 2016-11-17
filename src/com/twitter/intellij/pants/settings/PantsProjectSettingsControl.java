// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl;
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemSettingsControlCustomizer;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.BorderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PantsProjectSettingsControl extends AbstractExternalProjectSettingsControl<PantsProjectSettings> {
  private static final Logger LOG = Logger.getInstance(PantsProjectSettingsControl.class);
  private final boolean myShowAdvancedSettings;

  private CheckBoxList<String> myTargetSpecs;
  private JBCheckBox myWithDependeesCheckBox;
  private JBCheckBox myLibsWithSourcesCheckBox;
  private JBCheckBox myEnableIncrementalImport;

  public PantsProjectSettingsControl(@NotNull PantsProjectSettings settings, boolean showAdvancedSettings) {
    super(null, settings, new ExternalSystemSettingsControlCustomizer(true, true));
    myShowAdvancedSettings = showAdvancedSettings;
  }

  @Override
  protected void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel) {
    final JLabel hintLabel = new JBLabel(PantsBundle.message("pants.settings.text.path.hint"));
    hintLabel.setBorder(BorderFactory.createTitledBorder(PantsBundle.message("pants.settings.text.hint")));
    content.add(hintLabel, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

    myWithDependeesCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.dependents"));
    if (myShowAdvancedSettings) {
      content.add(myWithDependeesCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));
    }

    myLibsWithSourcesCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.sources.and.docs"));
    myLibsWithSourcesCheckBox.setSelected(false);
    content.add(myLibsWithSourcesCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

    myEnableIncrementalImport = new JBCheckBox(PantsBundle.message("pants.settings.text.with.incremental.import"));
    myEnableIncrementalImport.setSelected(false);
    content.add(myEnableIncrementalImport, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

    final JLabel targetsLabel = new JBLabel(PantsBundle.message("pants.settings.text.targets"));
    content.add(targetsLabel, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

    myTargetSpecs = new CheckBoxList<String>();
    content.add(ScrollPaneFactory.createScrollPane(myTargetSpecs), ExternalSystemUiUtil.getFillLineConstraints(0));
  }

  @Override
  protected boolean isExtraSettingModified() {
    return !myTargetSpecs.getSelectedValuesList().isEmpty();
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {
    final String externalProjectPath = getInitialSettings().getExternalProjectPath();
    if (!StringUtil.isEmpty(externalProjectPath)) {
      onProjectPathChanged(externalProjectPath);
    }
  }

  public void onProjectPathChanged(@NotNull final String projectPath) {
    myTargetSpecs.clear();
    final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    if (file == null || !PantsUtil.isPantsProjectFile(file)) {
      myTargetSpecs.setEnabled(true);
      LOG.warn("Bad project path: " + projectPath);
      return;
    }

    if (file.isDirectory()) {
      myTargetSpecs.setEnabled(false);
      myTargetSpecs.setEmptyText(PantsUtil.getRelativeProjectPath(new File(file.getPath())) + "/::");

      myWithDependeesCheckBox.setSelected(false);
      myWithDependeesCheckBox.setEnabled(true);

      myLibsWithSourcesCheckBox.setEnabled(true);
    }
    else if (PantsUtil.isExecutable(file.getPath())) {
      myTargetSpecs.setEnabled(false);
      myTargetSpecs.setEmptyText(file.getName());

      myWithDependeesCheckBox.setSelected(false);
      myWithDependeesCheckBox.setEnabled(false);

      myLibsWithSourcesCheckBox.setSelected(false);
      myLibsWithSourcesCheckBox.setEnabled(false);
    }
    else {
      myTargetSpecs.setEnabled(true);
      myTargetSpecs.setEmptyText(StatusText.DEFAULT_EMPTY_TEXT);

      myWithDependeesCheckBox.setSelected(false);
      myWithDependeesCheckBox.setEnabled(true);

      myLibsWithSourcesCheckBox.setEnabled(true);

      ProgressManager.getInstance().run(new Task.Modal(getProject(), PantsBundle.message("pants.getting.target.list"), false) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          loadTargets(projectPath);
        }
      });
    }
  }

  private void loadTargets(final String projectPath) {
    if (!PantsUtil.isBUILDFilePath(projectPath)) {
      return;
    }
    final Collection<String> targets = PantsUtil.listAllTargets(projectPath);
    UIUtil.invokeLaterIfNeeded(
      () -> {
        myTargetSpecs.clear();
        targets.forEach(s -> myTargetSpecs.addItem(s, s, false));
      }
    );
  }

  @Override
  protected void applyExtraSettings(@NotNull PantsProjectSettings settings) {
    final List<String> targetSpecs = new ArrayList<>();
    settings.setWithDependees(myWithDependeesCheckBox.isSelected());
    settings.setLibsWithSources(myLibsWithSourcesCheckBox.isSelected());
    settings.setEnableIncrementalImport(myEnableIncrementalImport.isSelected());
    for (int i = 0; i < myTargetSpecs.getItemsCount(); i++) {
      String target = myTargetSpecs.getItemAt(i);
      if (myTargetSpecs.isItemSelected(target)) {
        targetSpecs.add(target);
      }
    }
    settings.setTargetSpecs(targetSpecs);
  }

  @Override
  public boolean validate(@NotNull PantsProjectSettings settings) throws ConfigurationException {
    final String projectUrl = VfsUtil.pathToUrl(settings.getExternalProjectPath());
    final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(projectUrl);
    if (file == null) {
      throw new ConfigurationException(PantsBundle.message("pants.error.file.not.exists", projectUrl));
    }
    if (PantsUtil.isExecutable(file.getPath())) {
      return true;
    }
    if (!PantsUtil.isPantsProjectFile(file)) {
      throw new ConfigurationException(PantsBundle.message("pants.error.not.build.file.path.or.directory"));
    }
    if (PantsUtil.isBUILDFileName(file.getName()) && myTargetSpecs.getSelectedIndices().length == 0) {
      throw new ConfigurationException(PantsBundle.message("pants.error.no.targets.are.selected"));
    }
    return true;
  }
}
