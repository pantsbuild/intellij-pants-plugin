// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportBuilder;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.service.settings.ImportFromPantsControl;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;

public class PantsProjectImportBuilder extends AbstractExternalProjectImportBuilder<ImportFromPantsControl> {

  public PantsProjectImportBuilder(@NotNull ProjectDataManager dataManager) {
    super(dataManager, new ImportFromPantsControl(), PantsConstants.SYSTEM_ID);
  }

  @NotNull
  @Override
  public String getName() {
    return PantsBundle.message("pants.name");
  }

  @Override
  public Icon getIcon() {
    return PantsIcons.Icon;
  }

  @Override
  protected void doPrepare(@NotNull WizardContext context) {
    String pathToUse = context.getProjectFileDirectory();
    getControl(context.getProject()).setLinkedProjectPath(pathToUse);
  }

  @Override
  protected void beforeCommit(@NotNull DataNode<ProjectData> dataNode, @NotNull Project project) {

  }

  @NotNull
  @Override
  protected File getExternalProjectConfigToUse(@NotNull File file) {
    return file;
  }

  @Override
  protected void applyExtraSettings(@NotNull WizardContext context) {

  }
}
