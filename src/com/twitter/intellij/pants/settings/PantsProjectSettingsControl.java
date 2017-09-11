// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl;
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemSettingsControlCustomizer;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.UIUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PantsProjectSettingsControl extends AbstractExternalProjectSettingsControl<PantsProjectSettings> {

  @VisibleForTesting
  protected CheckBoxList<String> myTargetSpecsBox = new CheckBoxList<>();

  private JBCheckBox myLibsWithSourcesCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.sources.and.docs"));
  private JBCheckBox myEnableIncrementalImportCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.incremental.import"));
  private JBCheckBox myUseIdeaProjectJdkCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.jdk.enforcement"));

  @VisibleForTesting
  protected Set<String> errors = new HashSet<>();

  // Key to keep track whether target specs are requested for the same project path.
  private String lastPath = "";

  private PantsProjectSettings mySettings;

  public PantsProjectSettingsControl(@NotNull PantsProjectSettings settings) {
    super(null, settings, new ExternalSystemSettingsControlCustomizer(true, true));
    mySettings = settings;
  }

  @Override
  protected void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel) {

    myLibsWithSourcesCheckBox.setSelected(mySettings.isLibsWithSources());
    myEnableIncrementalImportCheckBox.setSelected(mySettings.isEnableIncrementalImport());
    myUseIdeaProjectJdkCheckBox.setSelected(mySettings.isUseIdeaProjectJdk());

    mySettings.getTargetSpecs().forEach(spec -> myTargetSpecsBox.addItem(spec, spec, true));

    List<JComponent> boxes = ContainerUtil.newArrayList(
      myLibsWithSourcesCheckBox,
      myEnableIncrementalImportCheckBox,
      myUseIdeaProjectJdkCheckBox,
      new JBLabel(PantsBundle.message("pants.settings.text.targets")),
      new JBScrollPane(myTargetSpecsBox)
    );

    GridBag lineConstraints = ExternalSystemUiUtil.getFillLineConstraints(indentLevel);

    for (JComponent component : boxes) {
      content.add(component, lineConstraints);
    }
  }

  // It is silly `CheckBoxList` does not provide an iterator.
  private List<String> getSelectedTargetSpecsFromBoxes() {
    List<String> selectedSpecs = Lists.newArrayList();
    for (int i = 0; i < myTargetSpecsBox.getModel().getSize(); i++) {
      JCheckBox checkBox = myTargetSpecsBox.getModel().getElementAt(i);
      if (checkBox.isSelected()) {
        selectedSpecs.add(checkBox.getText());
      }
    }
    return selectedSpecs;
  }

  @Override
  protected boolean isExtraSettingModified() {

    PantsProjectSettings newSettings = new PantsProjectSettings(
      getSelectedTargetSpecsFromBoxes(),
      // Project path is not visible to user, so it will stay the same.
      getInitialSettings().getExternalProjectPath(),
      myLibsWithSourcesCheckBox.isSelected(),
      myEnableIncrementalImportCheckBox.isSelected(),
      myUseIdeaProjectJdkCheckBox.isSelected()
    );

    return !newSettings.equals(getInitialSettings());
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {
  }

  public void onProjectPathChanged(@NotNull final String projectPath) {
    if (lastPath.equals(projectPath)) {
      return;
    }

    lastPath = projectPath;
    myTargetSpecsBox.clear();
    errors.clear();
    final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    if (file == null || !PantsUtil.isPantsProjectFile(file)) {
      myTargetSpecsBox.setEnabled(true);
      errors.add(String.format("Pants project not found given project path: %s", projectPath));
      return;
    }

    if (file.isDirectory()) {
      myTargetSpecsBox.setEnabled(false);
      Optional<String> relativeProjectPath = PantsUtil.getRelativeProjectPath(file.getPath());
      if (!relativeProjectPath.isPresent()) {
        errors.add(String.format("Fail to find relative path from %s to build root.", file.getPath()));
        return;
      }
      String spec = relativeProjectPath.get() + "/::";

      myTargetSpecsBox.setEmptyText(spec);
      myTargetSpecsBox.addItem(spec, spec, true);

      myLibsWithSourcesCheckBox.setEnabled(true);
    }
    else if (PantsUtil.isExecutable(file.getPath())) {
      myTargetSpecsBox.setEnabled(false);
      myTargetSpecsBox.setEmptyText(file.getName());

      myLibsWithSourcesCheckBox.setSelected(false);
      myLibsWithSourcesCheckBox.setEnabled(false);
    }
    else {
      myTargetSpecsBox.setEnabled(true);
      myTargetSpecsBox.setEmptyText(StatusText.DEFAULT_EMPTY_TEXT);
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
    try {
      final Collection<String> targets = PantsUtil.listAllTargets(projectPath);
      UIUtil.invokeLaterIfNeeded(() -> {
        myTargetSpecsBox.clear();
        targets.forEach(s -> myTargetSpecsBox.addItem(s, s, false));
      });
    } catch (PantsException e) {
      UIUtil.invokeLaterIfNeeded((Runnable) () -> {
        Messages.showErrorDialog(getProject(), e.getMessage(), "Pants Failure");
        Messages.createMessageDialogRemover(getProject()).run();
      });
    }
  }

  @Override
  protected void applyExtraSettings(@NotNull PantsProjectSettings settings) {
    settings.setLibsWithSources(myLibsWithSourcesCheckBox.isSelected());
    settings.setEnableIncrementalImport(myEnableIncrementalImportCheckBox.isSelected());
    settings.setUseIdeaProjectJdk(myUseIdeaProjectJdkCheckBox.isSelected());
    settings.setTargetSpecs(getSelectedTargetSpecsFromBoxes());
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
    if (PantsUtil.isBUILDFileName(file.getName()) && myTargetSpecsBox.getSelectedIndices().length == 0) {
      throw new ConfigurationException(PantsBundle.message("pants.error.no.targets.are.selected"));
    }
    if (!errors.isEmpty()) {
      String errorMessage = String.join("\n", errors);
      errors.clear();
      throw new ConfigurationException(errorMessage);
    }
    return true;
  }
}
