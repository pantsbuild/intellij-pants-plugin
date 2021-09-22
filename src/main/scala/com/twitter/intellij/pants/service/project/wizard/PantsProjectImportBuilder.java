// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.ProjectSdkData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportBuilder;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.projectImport.ProjectImportBuilder;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.settings.ImportFromPantsControl;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.io.File;
import java.util.Optional;

public class PantsProjectImportBuilder extends AbstractExternalProjectImportBuilder<ImportFromPantsControl> {

  public static PantsProjectImportBuilder getInstance(){
    return ProjectImportBuilder.EXTENSIONS_POINT_NAME.findExtensionOrFail(PantsProjectImportBuilder.class);
  }

  public PantsProjectImportBuilder() {
    super(ProjectDataManager.getInstance(), ImportFromPantsControl::new, PantsConstants.SYSTEM_ID);
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
    Optional.ofNullable(getExternalProjectNode())
      .map(node -> ExternalSystemApiUtil.find(node, ProjectSdkData.KEY))
      .map(DataNode::getData)
      .map(ProjectSdkData::getSdkName)
      .map(ProjectJdkTable.getInstance()::findJdk)
      .ifPresent(context::setProjectJdk);
  }
}
