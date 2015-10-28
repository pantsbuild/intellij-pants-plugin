// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;


import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.twitter.intellij.pants.PantsBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AddPythonPluginQuickFix extends PantsQuickFix {
  protected final PsiFile myPsiFile;

  public AddPythonPluginQuickFix(@NotNull PsiFile file) {
    myPsiFile = file;
  }

  @NotNull
  @Override
  public String getName() {
    return PantsBundle.message("quick.fix.add.python.plugin");
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  @NotNull
  @Override
  public String getText() {
    return PantsBundle.message("quick.fix.add.python.plugin");
  }

  @Override
  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void applyFix(Project project, ProblemDescriptor descriptor) {
    invoke(null, null, null);
  }

  @Override
  public void invoke(Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
    SwingUtilities.invokeLater(
      new Runnable() {
        public void run() {
          ShowSettingsUtil.getInstance().showSettingsDialog(myPsiFile.getProject(), "Plugins");
        }
      }
    );
  }
}