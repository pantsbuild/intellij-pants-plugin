// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.python.psi.*;
import com.twitter.intellij.pants.PantsBundle;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsPsiUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AddPantsTargetDependencyFix extends PantsQuickFix {
  protected final PantsTargetAddress myAddress;
  protected final PantsTargetAddress myAddressToAdd;

  public AddPantsTargetDependencyFix(@NotNull PantsTargetAddress address, @NotNull PantsTargetAddress addressToAdd) {
    myAddress = address;
    myAddressToAdd = addressToAdd;
  }

  @NotNull
  @Override
  public String getName() {
    return PantsBundle.message("quick.fix.add.target.dependency.description");
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  @NotNull
  @Override
  public String getText() {
    return PantsBundle.message("quick.fix.add.target.dependency.text", myAddressToAdd, myAddress);
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    final Module module = ModuleUtil.findModuleForPsiElement(file);
    return module != null && PantsUtil.findBUILDFileForModule(module) != null;
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    invoke(project, null, descriptor.getPsiElement().getContainingFile());
  }

  @Override
  public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiFile psiFile) throws IncorrectOperationException {
    final Module module = ModuleUtil.findModuleForPsiElement(psiFile);
    final VirtualFile buildFile = module != null ? PantsUtil.findBUILDFileForModule(module) : null;
    final PsiFile psiBuildFile = buildFile != null ? PsiManager.getInstance(project).findFile(buildFile) : null;
    if (psiBuildFile != null && StringUtil.isNotEmpty(myAddress.getTargetName())) {
      doInsert(psiBuildFile, myAddress.getTargetName(), myAddressToAdd);
    }
  }

  public void doInsert(
    @NotNull PsiFile buildFile,
    @NotNull final String targetName,
    @NotNull PantsTargetAddress addressToAdd
  ) throws IncorrectOperationException {
    final PyCallExpression targetDefinitionExpression = PantsPsiUtil.findTargets(buildFile).get(targetName);
    if (targetDefinitionExpression == null) {
      return;
    }

    final Project project = buildFile.getProject();
    final PyElementGenerator generator = PyElementGenerator.getInstance(project);
    final String targetAddressStringToAdd = addressToAdd.toString();

    final PyExpression dependenciesArgument = targetDefinitionExpression.getKeywordArgument("dependencies");
    if (dependenciesArgument == null) {
      final PyKeywordArgument keywordArgument =
        generator.createKeywordArgument(LanguageLevel.forElement(buildFile), "dependencies", "['"+ targetAddressStringToAdd + "']");
      targetDefinitionExpression.addArgument(keywordArgument);
    } else if (dependenciesArgument instanceof PyListLiteralExpression) {
      PyExpression position = null;
      // we assume all elements are sorted.
      for (PyExpression expression : ((PyListLiteralExpression)dependenciesArgument).getElements()) {
        if (expression instanceof PyStringLiteralExpression &&
            targetAddressStringToAdd.compareTo(((PyStringLiteralExpression)expression).getStringValue()) < 0) {
          // found a position to insert
          break;
        }
        position = expression;
      }
      final PyStringLiteralExpression literalToAdd = generator.createStringLiteralAlreadyEscaped("'" + targetAddressStringToAdd + "'");
      if (position != null) {
        final PsiElement newLine = PsiParserFacade.SERVICE.getInstance(project).createWhiteSpaceFromText("\n");
        final PsiElement addedLiteral = dependenciesArgument.addAfter(literalToAdd, position);
        dependenciesArgument.getNode().addChild(newLine.getNode(), addedLiteral.getNode());
      } else {
        dependenciesArgument.add(literalToAdd);
      }
      CodeStyleManager.getInstance(project).reformat(dependenciesArgument);
    }
    FileDocumentManager.getInstance().saveAllDocuments(); // dump VFS to FS before refreshing
    PantsUtil.refreshAllProjects(project);
  }
}
