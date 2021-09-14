// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileNameMatcher;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.twitter.intellij.pants.PantsBundle;
import org.jetbrains.annotations.NotNull;

public class TypeAssociationFix extends PantsQuickFix {
  @NotNull
  @Override
  public String getName() {
    return PantsBundle.message("quick.fix.add.build.file.treatment");
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  @NotNull
  @Override
  public String getText() {
    return PantsBundle.message("quick.fix.add.build.file.treatment");
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    invoke(project, null, descriptor.getPsiElement().getContainingFile());
  }

  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
    FileTypeManager manager = FileTypeManager.getInstance();
    FileType type = manager.getFileTypeByFileName(psiFile.getName());
    // Remove the BUILD file matcher from the wrong type then add it to PythonFileType
    for (FileNameMatcher matcher : manager.getAssociations(type)) {
      if (matcher.acceptsCharSequence(psiFile.getName())) {
        manager.removeAssociation(type, matcher);
      }
    }
  }
}