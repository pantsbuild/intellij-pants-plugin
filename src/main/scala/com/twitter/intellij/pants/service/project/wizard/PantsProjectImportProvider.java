// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectJdkStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.service.project.detector.ProjectType;
import com.twitter.intellij.pants.service.project.detector.SimpleProjectTypeDetector;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PantsProjectImportProvider extends AbstractExternalProjectImportProvider {
  private static final boolean CAN_BE_CANCELLED = true;

  static public String label() {
    return Optional
      .ofNullable(PropertiesComponent.getInstance().getValue("pants.import.provider.label"))
      .orElse("Pants");
  }

  public PantsProjectImportProvider() {
    super(PantsProjectImportBuilder.getInstance(), PantsConstants.SYSTEM_ID);
  }

  @Override
  public @NotNull
  @Nls(capitalization = Nls.Capitalization.Sentence) String getName() {
    return label();
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
    AtomicBoolean isSdkConfigured = new AtomicBoolean(true);
    String message = PantsBundle.message("pants.default.sdk.config.progress");
    ProgressManager.getInstance().run(new Task.Modal(context.getProject(), message, !CAN_BE_CANCELLED) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        if (isSdkConfigured.get()) {
          isSdkConfigured.set(isJvmProject(context.getProjectFileDirectory()));
        }
      }
    });
    if (isSdkConfigured.get()) {
      return super.createSteps(context);
    }
    return ArrayUtil.append(super.createSteps(context), new ProjectJdkStep(context));
  }

  // TODO check for Python project to python interpreter automatically.
  // https://github.com/pantsbuild/intellij-pants-plugin/issues/128
  private boolean isJvmProject(String rootPath) {
    if (PantsUtil.isExecutable(rootPath)) {
      return false;
    }

    try {
      return ProjectType.Jvm == SimpleProjectTypeDetector.create(new File(rootPath)).detect();
    } catch (IOException ex) {
      throw new PantsException(String.format("Failed detecting project type for %s", rootPath));
    }
  }
}
