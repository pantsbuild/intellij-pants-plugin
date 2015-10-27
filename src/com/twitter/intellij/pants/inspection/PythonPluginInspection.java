// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.inspection;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiFile;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PythonPluginInspection extends BaseJavaLocalInspectionTool {
  @Override
  @Nullable
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {

    if (PantsUtil.isBUILDFileName(file.getName()) && (!PantsUtil.isPythonAvailable())) {
      ProblemDescriptor descriptor = manager.createProblemDescriptor(
        file.getNavigationElement(),
        PantsBundle.message("pants.info.python.plugin.missing"),
        isOnTheFly,
        null,
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
      );
      return new ProblemDescriptor[]{descriptor};
    }
    return null;
  }
}
