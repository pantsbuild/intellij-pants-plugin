// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.inspection;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.facet.PythonFacet;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.quickfix.AddPythonFacetQuickFix;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PythonFacetInspection extends LocalInspectionTool {
  @Override
  @Nullable
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (PantsUtil.isBUILDFileName(file.getName()) &&
        !isPythonInModule(file) &&
        PantsUtil.isPantsProject(file.getProject())) {
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

  private boolean isPythonInModule(@NotNull PsiFile file) {
    final Module module = ModuleUtil.findModuleForPsiElement(file);
    if (module != null) {
      PythonFacet facet = FacetManager.getInstance(module).getFacetByType(PythonFacet.ID);
      return facet != null && facet.getConfiguration().getSdk() != null;
    }
    return false;
  }
}
