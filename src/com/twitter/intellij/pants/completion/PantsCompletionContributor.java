// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.twitter.intellij.pants.index.PantsBuildFileIndex;
import com.twitter.intellij.pants.index.PantsTargetIndex;
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
   *
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
      Collection<String> addresses = PantsBuildFileIndex.getFiles(psiFile);
      for (String address : addresses) {
        set.addElement(LookupElementBuilder.create(address));
      }
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
      ProcessingContext context,
      @NotNull CompletionResultSet result
    ) {
      final PsiFile psiFile = parameters.getOriginalFile();
      if (!PantsUtil.isBUILDFileName(psiFile.getName())) {
        return;
      }
      for (String alias : PantsTargetIndex.getTargets(psiFile.getProject())) {
        result.addElement(LookupElementBuilder.create(alias));
      }
    }
  }
}
