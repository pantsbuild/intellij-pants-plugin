package com.twitter.intellij.pants.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.twitter.intellij.pants.index.PantsTargetIndex;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * todo: remove dirty hack after PyPreferenceCompletionProvider patch is merged in IntelliJ
 */
public class PantsCompletionContributor extends CompletionContributor {
  public PantsCompletionContributor() {
    extend(
      CompletionType.BASIC,
      psiElement().withParent(PyReferenceExpression.class),
      new CompletionProvider<CompletionParameters>() {
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
    );
  }
}
