// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.wizard;

import java.io.File;

import com.google.common.collect.Multimap;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectJdkStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.twitter.intellij.pants.util.FileUtil;
import com.twitter.intellij.pants.util.FileUtil.SourceExtension;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nullable;

import static com.intellij.structuralsearch.impl.matcher.PatternTreeContext.File;

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
      PantsUtil.findPantsExecutable(context.getProjectFileDirectory()).getPath()) &&
      isJavaScalaProject(context.getProjectFileDirectory())) {
      return super.createSteps(context);
    }
    return ArrayUtil.append(super.createSteps(context), new ProjectJdkStep(context));
  }

  // TODO check if Python project using similar heuristic to set python interpreter
  // automatically. https://github.com/pantsbuild/intellij-pants-plugin/issues/128
  public boolean isJavaScalaProject(String rootPath) {
    if (PantsUtil.isExecutable(rootPath)) {
      return false;
    }

    File rootDirectory = new File(rootPath);
    if (PantsUtil.isBUILDFileName(rootDirectory.getName())) {
      rootDirectory = rootDirectory.getParentFile();
    }
    Multimap<SourceExtension, File> filesByExtension = FileUtil.find(rootDirectory);
    int numJavaScalaFiles = filesByExtension.get(SourceExtension.JAVA).size()
                            + filesByExtension.get(SourceExtension.SCALA).size();
    int numPyFiles = filesByExtension.get(SourceExtension.PY).size();

    if (numJavaScalaFiles > 0) {
      if (numPyFiles == 0) {
        return true;
      }
      // If there are many more java/scala files than py files, consider as Java/scala project too.
      if (numJavaScalaFiles * 1.0 / (numJavaScalaFiles + numPyFiles) > JAVA_SCALA_PROJECT_THRESHHOLD) {
        return true;
      }
    }

    return false;
  }

  private static final double JAVA_SCALA_PROJECT_THRESHHOLD = 0.90;
}
