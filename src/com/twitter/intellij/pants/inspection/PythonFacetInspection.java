// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.inspection;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiFile;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.quickfix.AddPythonFacetQuickFix;
import com.twitter.intellij.pants.util.PantsPythonSdkUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PythonFacetInspection extends LocalInspectionTool {
  @Override
  @Nullable
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (shouldAddPythonSdk(file)) {
      LocalQuickFix[] fixes = new LocalQuickFix[]{new AddPythonFacetQuickFix()};
      ProblemDescriptor descriptor = manager.createProblemDescriptor(
        file.getNavigationElement(),
        PantsBundle.message("pants.info.python.facet.missing"),
        isOnTheFly,
        fixes,
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
      );

      return new ProblemDescriptor[]{descriptor};
    }
    return null;
  }

  private boolean shouldAddPythonSdk(@NotNull PsiFile file) {
    if (!PantsUtil.isPantsProject(file.getProject())) {
      return false;
    }

    if (!PantsUtil.isBUILDFileName(file.getName())) {
      return false;
    }

    final Module module = ModuleUtil.findModuleForPsiElement(file);
    if (module == null) {
      return false;
    }

    return PantsPythonSdkUtil.hasNoPythonSdk(module);
  }
}
