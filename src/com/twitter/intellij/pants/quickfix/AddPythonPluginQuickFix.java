// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;


import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ui.UIUtil;
import com.twitter.intellij.pants.PantsBundle;
import org.jetbrains.annotations.NotNull;

public class AddPythonPluginQuickFix extends PantsQuickFix {
  @NotNull
  @Override
  public String getName() {
    return PantsBundle.message("quick.fix.add.python.plugin");
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @NotNull
  @Override
  public String getText() {
    return PantsBundle.message("quick.fix.add.python.plugin");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    invoke(null, null, null);
  }

  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
    UIUtil.invokeLaterIfNeeded(
      () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins")
    );
  }
}