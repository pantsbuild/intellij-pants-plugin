// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.PyStatement;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import com.twitter.intellij.pants.index.PantsAddressesIndex;
import com.twitter.intellij.pants.index.PantsTargetIndex;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * todo: remove dirty hack after PyPreferenceCompletionProvider patch is merged in IntelliJ
 */
public class PantsCompletionContributor extends CompletionContributor {
  public PantsCompletionContributor() {
    extend(
      CompletionType.BASIC,
      psiElement().withParent(PyReferenceExpression.class),
      new TargetTypeCompletionProvider()
    );

    extend(
      CompletionType.BASIC,
      psiElement(),
      new AbsoluteAddressesCompletionProvider()
    );
  }

  /**
   * A completion provider for absolute addresses.
   * Given a project with the following structure:
   * <ul>
   * <li>some/nested/tool/BUILD</li>
   * <li>another/far/away/tool/BUILD</li>
   * </ul>
   * This provider will add these two suggestions (regardless of the input):
   * <ul>
   * <li>`some/nested/tool`</li>
   * <li>`another/far/away/tool`</li>
   * </ul>
   * Note that the suggestions will be filtered by intellij further down the line.
   */
  private static class AbsoluteAddressesCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(
      @NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet set
    ) {

      final PsiFile psiFile = parameters.getOriginalFile();
      if (!PantsUtil.isBUILDFileName(psiFile.getName())) {
        return;
      }

      if (isDependenciesString(parameters)) {
        Collection<String> addresses = PantsAddressesIndex.getAddresses(psiFile);
        for (String address : addresses) {
          set.addElement(LookupElementBuilder.create(address).withIcon(AllIcons.Nodes.Module));
        }
      }
    }

    private boolean isDependenciesString(@NotNull CompletionParameters parameters) {
      PsiElement position = parameters.getPosition();
      PsiElement stringLiteral = position.getParent();
      if (!(stringLiteral instanceof PyStringLiteralExpression)) return false;
      if (stringLiteral.getParent() == null) return false;
      PsiElement dependencies = stringLiteral.getParent().getParent();
      return dependencies != null && dependencies.getText().startsWith("dependencies");
    }
  }

  /**
   * Provides completion for target types such as `java_library`.
   */
  // See PantsCompletionTest#testTargets
  private static class TargetTypeCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(
      @NotNull CompletionParameters parameters,
      @NotNull ProcessingContext context,
      @NotNull CompletionResultSet result
    ) {
      if (isTopLevelExpression(parameters)) {
        final PsiFile psiFile = parameters.getOriginalFile();
        if (!PantsUtil.isBUILDFileName(psiFile.getName())) {
          return;
        }
        for (String alias : PantsTargetIndex.getTargets(psiFile.getProject())) {
          result.addElement(LookupElementBuilder.create(alias));
        }
        String[] allBuildTypes = PropertiesComponent.getInstance().getValues(PantsConstants.PANTS_AVAILABLE_TARGETS_KEY);
        if (allBuildTypes != null) {
          for (String targetType : allBuildTypes) {
            result.addElement(createAvailableTypeElement(targetType));
          }
        }
      }
    }

    LookupElement createAvailableTypeElement(String targetType) {
      return LookupElementBuilder
        .create(targetType + "(")
        .withInsertHandler((context, element1) -> {
          context.getDocument().insertString(context.getSelectionEndOffset(), ")");
          context.commitDocument();
        })
        .withIcon(AllIcons.Nodes.Method)
        .withTailText(")");
    }

    private boolean isTopLevelExpression(@NotNull CompletionParameters parameters) {
      PsiElement position = parameters.getPosition();
      PsiElement expression = position.getParent();
      if (!(expression instanceof PyExpression)) return false;
      PsiElement statement = expression.getParent();
      if (!(statement instanceof PyStatement)) return false;
      PsiElement file = statement.getParent();
      return file instanceof PyFile;
    }
  }
}