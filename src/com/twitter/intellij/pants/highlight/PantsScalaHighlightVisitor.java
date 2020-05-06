// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.highlight;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightVisitor;
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.twitter.intellij.pants.quickfix.PantsQuickFix;
import com.twitter.intellij.pants.quickfix.PantsUnresolvedReferenceFixFinder;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.annotator.createFromUsage.CreateTypeDefinitionQuickFix;
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile;

import java.util.List;

/**
 * A hack to extend fixes in Scala code. @fkorotkov asked JetBrains to add an extension point as we have for Java
 */
public class PantsScalaHighlightVisitor implements HighlightVisitor {
  private HighlightInfoHolder myHolder;

  @Override
  public boolean suitableForFile(@NotNull PsiFile file) {
    return PantsUtil.isPythonAvailable() && file instanceof ScalaFile;
  }

  @Override
  public void visit(@NotNull PsiElement element) {
    // FIXME: Re-enable quick fix for missing dependencies once it is functional again.
    // https://github.com/pantsbuild/intellij-pants-plugin/issues/280
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null || DumbService.getInstance(myHolder.getProject()).isDumb()) {
      return;
    }
    int infoSize = myHolder.size();
    for (int i = 0; i < infoSize; i++) {
      final HighlightInfo info = myHolder.get(i);
      tryToExtendInfo(info, containingFile);
    }
  }

  private void tryToExtendInfo(@NotNull HighlightInfo info, @NotNull PsiFile containingFile) {
    List<Pair<HighlightInfo.IntentionActionDescriptor, TextRange>> actionRanges = info.quickFixActionRanges;
    if (actionRanges == null) {
      return;
    }
    for (Pair<HighlightInfo.IntentionActionDescriptor, TextRange> actionAndRange : actionRanges) {
      final TextRange textRange = actionAndRange.getSecond();
      final HighlightInfo.IntentionActionDescriptor actionDescriptor = actionAndRange.getFirst();
      final IntentionAction action = actionDescriptor.getAction();
      if (action instanceof CreateTypeDefinitionQuickFix) {
        final String className = textRange.substring(containingFile.getText());
        final List<PantsQuickFix> missingDependencyFixes =
          PantsUnresolvedReferenceFixFinder.findMissingDependencies(className, containingFile);
        for (PantsQuickFix fix : missingDependencyFixes) {
          info.registerFix(fix, null, fix.getName(), textRange, null);
        }
        if (!missingDependencyFixes.isEmpty()) {
          // we should add only one fix per info
          return;
        }
      }
    }
  }

  @Override
  public boolean analyze(
    @NotNull PsiFile file,
    boolean updateWholeFile,
    @NotNull HighlightInfoHolder holder,
    @NotNull Runnable action
  ) {
    myHolder = holder;
    try {
      action.run();
    }
    finally {
      myHolder = null;
    }
    return true;
  }

  @NotNull
  @Override
  public HighlightVisitor clone() {
    return new PantsScalaHighlightVisitor();
  }
}
