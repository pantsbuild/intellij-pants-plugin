// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectJdkStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nullable;

public class PantsProjectImportProvider extends AbstractExternalProjectImportProvider {
  public PantsProjectImportProvider(PantsProjectImportBuilder builder) {
    super(builder, PantsConstants.SYSTEM_ID);
  }

  @Override
  protected boolean canImportFromFile(VirtualFile file) {
    return PantsUtil.isBUILDFileName(file.getName()) || PantsUtil.isExecutable(file.getCanonicalPath());
  }

  @Nullable
  @Override
  public String getFileSample() {
    return "<b>Pants</b> build file (BUILD.*) or a script";
  }

  @Override
  public ModuleWizardStep[] createSteps(WizardContext context) {
    /**
     * Newer export version project sdk can be automatically discovered and configured.
     */
    if (PantsUtil.supportExportDefaultJavaSdk(
      PantsUtil.findPantsExecutable(context.getProjectFileDirectory()).getPath())) {
      return super.createSteps(context);
    }
    return ArrayUtil.append(super.createSteps(context), new ProjectJdkStep(context));
  }
}
