// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.inspection;

import com.intellij.codeInspection.*;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.PythonFileType;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.quickfix.TypeAssociationFix;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.fileTypes.FileTypeRegistry;

public class BuildFileTypeInspection extends LocalInspectionTool {
  @Override
  @Nullable
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (PantsUtil.isBUILDFileName(file.getName()) && PantsUtil.isPythonAvailable() && PantsUtil.isPantsProject(file.getProject())) {
      if (file.getFileType() != PythonFileType.INSTANCE) {
        LocalQuickFix[] fixes = new LocalQuickFix[]{new TypeAssociationFix()};
        ProblemDescriptor descriptor = manager.createProblemDescriptor(
          file.getNavigationElement(),
          PantsBundle.message("pants.info.mistreated.build.file"),
          isOnTheFly,
          fixes,
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        );
        return new ProblemDescriptor[]{descriptor};
      }
    }
    return null;
  }
}