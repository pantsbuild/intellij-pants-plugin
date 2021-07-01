// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl;
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
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.StatusText;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PantsProjectSettingsControl extends AbstractExternalProjectSettingsControl<PantsProjectSettings> {

  private enum ProjectPathFileType {
    isNonExistent,
    isNonPantsFile,
    isRecursiveDirectory,
    executableScript,
    isBUILDFile
  }

  private final JTextField myNameField = new JTextField();

  private JSpinner newImportDepthSpinner(){
    JSpinner spinner = new JSpinner();
    spinner.setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
    spinner.setEnabled(false);
    return spinner;
  }

  private JBCheckBox newEnableIcrementalImportCheckbox(){
    JBCheckBox checkbox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.incremental.import"));
    checkbox.addActionListener(evt -> {
      int defaultIncrementalImportDepth = 10;
      myImportDepthSpinner.setValue(((JBCheckBox) evt.getSource()).isSelected() ? defaultIncrementalImportDepth : 0);
      myImportDepthSpinner.setEnabled(((JBCheckBox) evt.getSource()).isSelected());
    });
    return checkbox;
  }

  @VisibleForTesting
  protected CheckBoxList<String> myTargetSpecsBox = new CheckBoxList<>();

  private JBCheckBox myLibsWithSourcesCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.sources.and.docs"));
  private JSpinner myImportDepthSpinner = newImportDepthSpinner();
  private JBCheckBox myEnableIncrementalImportCheckBox = newEnableIcrementalImportCheckbox();
  private JBCheckBox myUseIdeaProjectJdkCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.with.jdk.enforcement"));
  private JBCheckBox myImportSourceDepsAsJarsCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.import.deps.as.jars"));
  private JBCheckBox myUseIntellijCompilerCheckBox = new JBCheckBox(PantsBundle.message("pants.settings.text.use.intellij.compiler"));
  private JPanel myImportDepthPanel = importDepthPanel(myImportDepthSpinner);

  @VisibleForTesting
  protected Set<String> errors = new HashSet<>();

  // Key to keep track whether target specs are requested for the same project path.
  private String lastPath = "";
  private String lastGeneratedName = "";

  PantsProjectSettingsControl(@NotNull PantsProjectSettings settings) {
    super(null, settings);
  }

  private static JPanel importDepthPanel(JSpinner importDepthSpinner) {
    JPanel importDepthPanel = new JPanel();
    importDepthPanel.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    importDepthPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
    importDepthPanel.add(new JLabel("Import depth: "));
    importDepthPanel.add(importDepthSpinner);
    return importDepthPanel;

  }

  @Override
  protected void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel) {
    PantsProjectSettings initialSettings = getInitialSettings();
    myLibsWithSourcesCheckBox.setSelected(initialSettings.libsWithSources);
    myEnableIncrementalImportCheckBox.setSelected(initialSettings.incrementalImportEnabled);
    myImportDepthSpinner.setValue(initialSettings.incrementalImportDepth);
    myUseIdeaProjectJdkCheckBox.setSelected(initialSettings.useIdeaProjectJdk);
    myImportSourceDepsAsJarsCheckBox.setSelected(initialSettings.importSourceDepsAsJars);
    myUseIntellijCompilerCheckBox.setSelected(initialSettings.useIntellijCompiler);
    LinkLabel<?> intellijCompilerHelpMessage = LinkLabel.create(
      PantsBundle.message("pants.settings.text.use.intellij.compiler.help.messasge"),
      () -> BrowserUtil.browse(PantsBundle.message("pants.settings.text.use.intellij.compiler.help.messasge.link"))
    );

    myTargetSpecsBox.setItems(initialSettings.getAllAvailableTargetSpecs(), x -> x);
    initialSettings.getSelectedTargetSpecs().forEach(spec -> myTargetSpecsBox.setItemSelected(spec, true));

    insertNameFieldBeforeProjectPath(content);

    List<JComponent> boxes = ContainerUtil.newArrayList(
      myLibsWithSourcesCheckBox,
      myEnableIncrementalImportCheckBox,
      myImportDepthPanel,
      myUseIdeaProjectJdkCheckBox,
      myImportSourceDepsAsJarsCheckBox,
      myUseIntellijCompilerCheckBox,
      intellijCompilerHelpMessage,
      new JBLabel(PantsBundle.message("pants.settings.text.targets")),
      new JBScrollPane(myTargetSpecsBox)
    );

    GridBag lineConstraints = ExternalSystemUiUtil.getFillLineConstraints(indentLevel);

    for (JComponent component : boxes) {
      content.add(component, lineConstraints);
    }
  }

  private void insertNameFieldBeforeProjectPath(@NotNull PaintAwarePanel content) {
    JLabel label = new JLabel(PantsBundle.message("pants.settings.text.project.name"));
    content.add(label, ExternalSystemUiUtil.getLabelConstraints(0), 0);
    content.add(myNameField, ExternalSystemUiUtil.getFillLineConstraints(0), 1);
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
      getAllTargetSpecsFromBoxes(),
      // Project path is not visible to user, so it will stay the same.
      getInitialSettings().getExternalProjectPath(),
      myLibsWithSourcesCheckBox.isSelected(),
      myEnableIncrementalImportCheckBox.isSelected(),
      (Integer)(myImportDepthSpinner.getValue()),
      myUseIdeaProjectJdkCheckBox.isSelected(),
      myImportSourceDepsAsJarsCheckBox.isSelected(),
      myUseIntellijCompilerCheckBox.isSelected()
    );

    newSettings.setProjectName(myNameField.getText());
    return !newSettings.equals(getInitialSettings());
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {
    // NB: This is called when a new run of the import wizard happens.
    //     The values of all the settings are either their initial values,
    //     or whatever they were last set to, so you can't reuse them.
    lastPath = "";
    lastGeneratedName = "";
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
    String buildRoot = PantsUtil.findBuildRoot(file).map(VirtualFile::getName).orElse("");

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
          String relativePath = relativeProjectPath.get();
          if (relativePath.equals(".")) {
            setGeneratedName(buildRoot);
          }
          else {
            setGeneratedName(buildRoot + File.separator + relativePath);
          }

          String spec = relativePath + "/::";
          myTargetSpecsBox.setEmptyText(spec);
          myTargetSpecsBox.addItem(spec, spec, true);

          myLibsWithSourcesCheckBox.setEnabled(true);
        } else {
          clearGeneratedName();
          errors.add(String.format("Fail to find relative path from %s to build root.", file.getPath()));
          return;
        }
        break;

      case executableScript:
        String path = PantsUtil.getRelativeProjectPath(file.getPath()).orElse(file.getName());
        if (path.equals(".")) {
          setGeneratedName(buildRoot + File.separator + file.getName());
        }
        else {
          setGeneratedName(buildRoot + File.separator + path);
        }

        myTargetSpecsBox.setEnabled(false);
        myTargetSpecsBox.setEmptyText(path);

        myLibsWithSourcesCheckBox.setSelected(false);
        myLibsWithSourcesCheckBox.setEnabled(false);
        break;

      case isBUILDFile:
        String name = PantsUtil.getRelativeProjectPath(file.getPath())
          .orElse(file.getParent().getName());

        if(name.equals(".")) {
          setGeneratedName(buildRoot);
        }else {
          setGeneratedName(buildRoot + File.separator + name);
        }
        myTargetSpecsBox.setEnabled(true);
        myTargetSpecsBox.setEmptyText(StatusText.getDefaultEmptyText());
        myLibsWithSourcesCheckBox.setEnabled(true);

        ProgressManager.getInstance().run(new Task.Modal(getProject(),
            PantsBundle.message("pants.getting.target.list"), false) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            try {
              final Collection<String> targets = PantsUtil.listAllTargets(projectPath);
              PantsUtil.invokeLaterIfNeeded(() -> {
                targets.forEach(s -> myTargetSpecsBox.addItem(s, s, false));
              });
            }
            catch (RuntimeException e) {
              PantsUtil.invokeLaterIfNeeded(() -> {
                Messages.showErrorDialog(getProject(), e.getMessage(), "Pants Failure");
                Messages.createMessageDialogRemover(getProject()).run();
              });
            }
          }
        });
        break;
      default:
        clearGeneratedName();
        PantsUtil.invokeLaterIfNeeded(() -> {
          Messages.showErrorDialog(getProject(), "Unexpected project file state: " + pathFileType, "Pants Failure");
          Messages.createMessageDialogRemover(getProject()).run();
        });
    }
  }

  private void clearGeneratedName() {
    setGeneratedName("");
  }

  private void setGeneratedName(String name) {
    boolean notChangedByTheUser = lastGeneratedName.equals(myNameField.getText());
    if (lastGeneratedName.isEmpty() || notChangedByTheUser) {
      String escapedName = name.replace(File.separator, ".");
      myNameField.setText(escapedName);
      lastGeneratedName = escapedName;
    }
  }

  @Override
  protected void applyExtraSettings(@NotNull PantsProjectSettings settings) {
    settings.setProjectName(myNameField.getText());
    settings.setSelectedTargetSpecs(getSelectedTargetSpecsFromBoxes());
    settings.setAllAvailableTargetSpecs(getAllTargetSpecsFromBoxes());
    settings.libsWithSources = myLibsWithSourcesCheckBox.isSelected();
    settings.incrementalImportEnabled = myEnableIncrementalImportCheckBox.isSelected();
    settings.incrementalImportDepth = (Integer) (myImportDepthSpinner.getValue());
    settings.useIdeaProjectJdk = myUseIdeaProjectJdkCheckBox.isSelected();
    settings.importSourceDepsAsJars = myImportSourceDepsAsJarsCheckBox.isSelected();
    settings.useIntellijCompiler = myUseIntellijCompilerCheckBox.isSelected();
  }

  @NotNull
  private List<String> getAllTargetSpecsFromBoxes() {
    return IntStream.range(0, myTargetSpecsBox.getItemsCount()).mapToObj(i -> myTargetSpecsBox.getItemAt(i)).collect(
      Collectors.toList());
  }

  @Override
  public boolean validate(@NotNull PantsProjectSettings settings) throws ConfigurationException {
    if(myNameField.getText().isEmpty()){
      throw new ConfigurationException(PantsBundle.message("pants.error.project.name.empty"));
    }

    if (myNameField.getText().length() > 200) {
      throw new ConfigurationException(PantsBundle.message("pants.error.project.name.tooLong"));
    }

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
