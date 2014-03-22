package com.twitter.intellij.pants.settings;

import com.intellij.openapi.externalSystem.model.settings.LocationSettingType;
import com.intellij.openapi.externalSystem.service.settings.AbstractExternalProjectSettingsControl;
import com.intellij.openapi.externalSystem.util.ExternalSystemUiUtil;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.components.JBLabel;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class PantsProjectSettingsControl extends AbstractExternalProjectSettingsControl<PantsProjectSettings> {
  private JLabel myPantsHomeLabel;
  private TextFieldWithBrowseButton myPantsExecutablePathField;

  public PantsProjectSettingsControl(@NotNull PantsProjectSettings settings) {
    super(settings);
  }

  @Override
  protected void fillExtraControls(@NotNull PaintAwarePanel content, int indentLevel) {
    initControls();

    content.add(myPantsHomeLabel, ExternalSystemUiUtil.getLabelConstraints(indentLevel));
    content.add(myPantsExecutablePathField, ExternalSystemUiUtil.getFillLineConstraints(0));
  }

  private void initControls() {
    myPantsHomeLabel = new JBLabel(PantsBundle.message("pants.settings.text.executable.path"));

    myPantsExecutablePathField = new TextFieldWithBrowseButton();

    myPantsExecutablePathField.addBrowseFolderListener(
      "",
      PantsBundle.message("gradle.settings.text.executable.path"),
      null,
      PantsUtil.PANTS_FILE_CHOOSER_DESCRIPTOR,
      TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT,
      false
    );
    myPantsExecutablePathField.getTextField().getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        myPantsExecutablePathField.getTextField().setForeground(LocationSettingType.EXPLICIT_CORRECT.getColor());
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        myPantsExecutablePathField.getTextField().setForeground(LocationSettingType.EXPLICIT_CORRECT.getColor());
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
      }
    });
  }

  @Override
  protected boolean isExtraSettingModified() {
    final String pantsExecutablePath = FileUtil.toCanonicalPath(myPantsExecutablePathField.getText());
    return !StringUtil.isEmpty(pantsExecutablePath);
  }

  @Override
  protected void resetExtraSettings(boolean isDefaultModuleCreation) {
    myPantsExecutablePathField.setText("");
  }

  @Override
  protected void applyExtraSettings(@NotNull PantsProjectSettings settings) {
    final String pantsExecutablePath = settings.getPantsExecutablePath();
    if (pantsExecutablePath != null) {
      myPantsExecutablePathField.setText(FileUtil.toSystemIndependentName(pantsExecutablePath));
    }
  }

  @Override
  public boolean validate(@NotNull PantsProjectSettings settings) throws ConfigurationException {
    if (StringUtil.isEmpty(settings.getPantsExecutablePath())) {
      throw new ConfigurationException("Pants executable path is empty!");
    }
    final String executableUrl = VfsUtil.pathToUrl(settings.getPantsExecutablePath());
    final VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(executableUrl);
    if (fileByUrl == null) {
      throw new ConfigurationException("Couldn't find pants executable!");
    }
    return true;
  }
}
