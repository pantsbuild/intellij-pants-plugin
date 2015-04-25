// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl;
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemSettingsControlCustomizer;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class PantsProjectSettingsControl extends AbstractExternalProjectSettingsControl<PantsProjectSettings> {
  private static final Logger LOG = Logger.getInstance(PantsProjectSettingsControl.class);

  private CheckBoxList<String> myTargets;
  private JBCheckBox myWithDependeesCheckBox;

  public PantsProjectSettingsControl(@NotNull PantsProjectSettings settings) {
    super(null, settings, new ExternalSystemSettingsControlCustomizer(true, true));
  }

  @Override
  protected void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel) {
    final JLabel targetsLabel = new JBLabel(PantsBundle.message("pants.settings.text.targets"));
    myTargets = new CheckBoxList<String>();

    myWithDependeesCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.dependees"));
    content.add(myWithDependeesCheckBox, ExternalSystemUiUtil.getFillLineConstraints(indentLevel));

    content.add(targetsLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
    content.add(myTargets, ExternalSystemUiUtil.getFillLineConstraints(0));
  }

  @Override
  protected boolean isExtraSettingModified() {
    return !myTargets.getSelectedValuesList().isEmpty();
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {
    final String externalProjectPath = getInitialSettings().getExternalProjectPath();
    if (!StringUtil.isEmpty(externalProjectPath)) {
      onProjectPathChanged(externalProjectPath);
    }
  }

  public void onProjectPathChanged(@NotNull final String projectPath) {
    myTargets.clear();
    final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    if (file == null || !PantsUtil.isPantsProjectFolder(file)) {
      myTargets.setEnabled(true);
      LOG.warn("Bad project path: " + projectPath);
      return;
    }

    if (file.isDirectory()) {
      myWithDependeesCheckBox.setSelected(false);
      myWithDependeesCheckBox.setEnabled(true);
    } else if (PantsUtil.isExecutable(file.getPath())) {
      myTargets.setEnabled(false);

      myWithDependeesCheckBox.setSelected(false);
      myWithDependeesCheckBox.setEnabled(false);
    } else {
      myWithDependeesCheckBox.setSelected(false);
      myWithDependeesCheckBox.setEnabled(true);
      loadTargets(projectPath);
    }
  }

  private void loadTargets(final String projectPath) {
    myTargets.setPaintBusy(true);
    ProgressManager.getInstance().run(
      new Task.Backgroundable(
        null, PantsBundle.message("pants.settings.text.loading.targets"),
        false, PerformInBackgroundOption.ALWAYS_BACKGROUND
      ) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          try {
            final List<String> targets = PantsUtil.listAllTargets(projectPath);
            UIUtil.invokeLaterIfNeeded(
              new Runnable() {
                @Override
                public void run() {
                  myTargets.clear();
                  for (String target : targets) {
                    myTargets.addItem(target, target, false);
                  }
                }
              }
            );
          } finally {
            myTargets.setPaintBusy(false);
          }
        }
      }
    );
  }

  @Override
  protected void applyExtraSettings(@NotNull PantsProjectSettings settings) {
    final List<String> result = new ArrayList<String>();
    settings.setWithDependees(myWithDependeesCheckBox.isSelected());
    for (int i = 0; i < myTargets.getItemsCount(); i++) {
      String target = myTargets.getItemAt(i);
      if (myTargets.isItemSelected(target)) {
        result.add(target);
      }
    }
    settings.setTargetNames(result);
  }

  @Override
  public boolean validate(@NotNull PantsProjectSettings settings) throws ConfigurationException {
    final String projectUrl = VfsUtil.pathToUrl(settings.getExternalProjectPath());
    final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(projectUrl);
    if (file == null) {
      throw new ConfigurationException(PantsBundle.message("pants.error.file.not.exists"));
    }
    if (!PantsUtil.isPantsProjectFolder(file)) {
      throw new ConfigurationException(PantsBundle.message("pants.error.not.build.file.path.or.directory"));
    }
    if (PantsUtil.isBUILDFileName(file.getName()) && myTargets.getSelectedIndices().length == 0) {
      throw new ConfigurationException(PantsBundle.message("pants.error.no.targets.are.selected"));
    }
    return true;
  }
}
