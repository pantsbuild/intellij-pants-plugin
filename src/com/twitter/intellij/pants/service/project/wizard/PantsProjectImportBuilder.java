// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportBuilder;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.service.settings.ImportFromPantsControl;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.List;

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
    final DataNode<ProjectData> node = getExternalProjectNode();
    if (node == null) {
      return;
    }

    final DataNode<Sdk> sdkNode = ExternalSystemApiUtil.find(node, PantsConstants.SDK_KEY);
    if (sdkNode != null) {
      Sdk pantsSdk = sdkNode.getData();
      context.setProjectJdk(addIfNotExists(pantsSdk));
    }
  }

  /**
   * Find if pants sdk is already configured, return the existing sdk if it exists,
   * otherwise add to the config and return.
   */
  private Sdk addIfNotExists(final Sdk pantsSdk) {
    final JavaSdk javaSdk = JavaSdk.getInstance();
    List<Sdk> sdks = ProjectJdkTable.getInstance().getSdksOfType(javaSdk);
    for (Sdk sdk : sdks) {
      if (javaSdk.getVersion(sdk) == javaSdk.getVersion(pantsSdk)) {
        return sdk;
      }
    }
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        ProjectJdkTable.getInstance().addJdk(pantsSdk);
      }
    });
    return pantsSdk;
  }
}
