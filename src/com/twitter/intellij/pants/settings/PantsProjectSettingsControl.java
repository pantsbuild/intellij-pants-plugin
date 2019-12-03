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

  private enum ProjectPathFileType {
    isNonExistent,
    isNonPantsFile,
    isRecursiveDirectory,
    executableScript,
    isBUILDFile
  }

  @VisibleForTesting
  protected CheckBoxList<String> myTargetSpecsBox = new CheckBoxList<>();

  private JBCheckBox myLibsWithSourcesCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.sources.and.docs"));
  private JBCheckBox myEnableIncrementalImportCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.incremental.import"));
  private JBCheckBox myUseIdeaProjectJdkCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.jdk.enforcement"));
  private JBCheckBox myImportSourceDepsAsJarsCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.import.deps.as.jars"));
  private JBCheckBox myUseIntellijCompilerCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.use.intellij.compiler"));

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

    myLibsWithSourcesCheckBox.setSelected(mySettings.libsWithSources);
    myEnableIncrementalImportCheckBox.setSelected(mySettings.enableIncrementalImport);
    myUseIdeaProjectJdkCheckBox.setSelected(mySettings.useIdeaProjectJdk);
    myImportSourceDepsAsJarsCheckBox.setSelected(mySettings.importSourceDepsAsJars);
    myUseIntellijCompilerCheckBox.setSelected(mySettings.useIntellijCompiler);

    mySettings.getTargetSpecs().forEach(spec -> myTargetSpecsBox.addItem(spec, spec, true));

    List<JComponent> boxes = ContainerUtil.newArrayList(
      myLibsWithSourcesCheckBox,
      myEnableIncrementalImportCheckBox,
      myUseIdeaProjectJdkCheckBox,
      myImportSourceDepsAsJarsCheckBox,
      myUseIntellijCompilerCheckBox,
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
      myUseIdeaProjectJdkCheckBox.isSelected(),
      myImportSourceDepsAsJarsCheckBox.isSelected(),
      myUseIntellijCompilerCheckBox.isSelected()
    );

    return !newSettings.equals(getInitialSettings());
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {
    // NB: This is called when a new run of the import wizard happens.
    //     The values of all the settings are either their initial values,
    //     or whatever they were last set to, so you can't reuse them.
    lastPath = "";
    myTargetSpecsBox.clear();
    errors.clear();
  }

  public void onProjectPathChanged(@NotNull final String projectPath) {
    // NB: onProjectPathChanged is called twice for each path. This guard ensures we only run it
    //     once per set of calls.
    if (lastPath.equals(projectPath)) {
      return;
    }
    lastPath = projectPath;

    myTargetSpecsBox.clear();
    errors.clear();

    final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    ProjectPathFileType pathFileType = determinePathKind(file);
    switch (pathFileType) {
      case isNonExistent:
      case isNonPantsFile:
        myTargetSpecsBox.setEnabled(true);
        errors.add(String.format("Pants project not found given project path: %s", projectPath));
        break;

      case isRecursiveDirectory:
        myTargetSpecsBox.setEnabled(false);
        Optional<String> relativeProjectPath = PantsUtil.getRelativeProjectPath(file.getPath());

        if (relativeProjectPath.isPresent()) {
          String spec = relativeProjectPath.get() + "/::";

          myTargetSpecsBox.setEmptyText(spec);
          myTargetSpecsBox.addItem(spec, spec, true);

          myLibsWithSourcesCheckBox.setEnabled(true);
        } else {
          errors.add(String.format("Fail to find relative path from %s to build root.", file.getPath()));
          return;
        }
        break;

      case executableScript:
        myTargetSpecsBox.setEnabled(false);
        myTargetSpecsBox.setEmptyText(PantsUtil.getRelativeProjectPath(file.getPath()).orElse(file.getName()));

        myLibsWithSourcesCheckBox.setSelected(false);
        myLibsWithSourcesCheckBox.setEnabled(false);
        break;

      case isBUILDFile:
        myTargetSpecsBox.setEnabled(true);
        myTargetSpecsBox.setEmptyText(StatusText.DEFAULT_EMPTY_TEXT);
        myLibsWithSourcesCheckBox.setEnabled(true);

        ProgressManager.getInstance().run(new Task.Modal(getProject(),
            PantsBundle.message("pants.getting.target.list"), false) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            try {
              final Collection<String> targets = PantsUtil.listAllTargets(projectPath);
              UIUtil.invokeLaterIfNeeded(() -> {
                targets.forEach(s -> myTargetSpecsBox.addItem(s, s, false));
              });
            } catch (RuntimeException e) {
              UIUtil.invokeLaterIfNeeded((Runnable) () -> {
                Messages.showErrorDialog(getProject(), e.getMessage(), "Pants Failure");
                Messages.createMessageDialogRemover(getProject()).run();
              });
            }
          }
        });
        break;
      default:
        UIUtil.invokeLaterIfNeeded((Runnable) () -> {
          Messages.showErrorDialog(getProject(), "Unexpected project file state: " + pathFileType, "Pants Failure");
          Messages.createMessageDialogRemover(getProject()).run();
        });
    }
  }

  @Override
  protected void applyExtraSettings(@NotNull PantsProjectSettings settings) {
    settings.setTargetSpecs(getSelectedTargetSpecsFromBoxes());
    settings.libsWithSources = myLibsWithSourcesCheckBox.isSelected();
    settings.enableIncrementalImport = myEnableIncrementalImportCheckBox.isSelected();
    settings.useIdeaProjectJdk = myUseIdeaProjectJdkCheckBox.isSelected();
    settings.importSourceDepsAsJars = myImportSourceDepsAsJarsCheckBox.isSelected();
    settings.useIntellijCompiler = myUseIntellijCompilerCheckBox.isSelected();
  }

  @Override
  public boolean validate(@NotNull PantsProjectSettings settings) throws ConfigurationException {
    final String projectUrl = VfsUtil.pathToUrl(settings.getExternalProjectPath());
    final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(projectUrl);
    ProjectPathFileType pathFileType = determinePathKind(file);
    switch (pathFileType) {
      case isNonExistent:
        throw new ConfigurationException(PantsBundle.message("pants.error.file.not.exists", projectUrl));
      case isNonPantsFile:
        throw new ConfigurationException(PantsBundle.message("pants.error.not.build.file.path.or.directory"));
      case executableScript:
        return true;
      case isBUILDFile:
        if (myTargetSpecsBox.getSelectedIndices().length == 0) {
          throw new ConfigurationException(PantsBundle.message("pants.error.no.targets.are.selected"));
        }
        break;
      case isRecursiveDirectory:
        break;
      default:
        throw new ConfigurationException("Unexpected project file state: " + pathFileType);
    }
    if (!errors.isEmpty()) {
      String errorMessage = String.join("\n", errors);
      errors.clear();
      throw new ConfigurationException(errorMessage);
    }
    return true;
  }

  private ProjectPathFileType determinePathKind(VirtualFile projectFile) {
    if (projectFile == null) {
      return ProjectPathFileType.isNonExistent;
    } else if (PantsUtil.isExecutable(projectFile.getPath())) {
      return ProjectPathFileType.executableScript;
    } else if (!PantsUtil.isPantsProjectFile(projectFile)) {
      return ProjectPathFileType.isNonPantsFile;
    } else if (projectFile.isDirectory()) {
      return ProjectPathFileType.isRecursiveDirectory;
    } else {
      return ProjectPathFileType.isBUILDFile;
    }
  }
}
