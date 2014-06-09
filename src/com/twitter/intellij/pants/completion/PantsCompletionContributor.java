package com.twitter.intellij.pants.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.QualifiedName;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.PyUtil;
import com.jetbrains.python.psi.resolve.CompletionVariantsProcessor;
import com.jetbrains.python.psi.resolve.PyResolveUtil;
import com.jetbrains.python.psi.resolve.ResolveImportUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
          if (!PantsUtil.BUILD.equals(psiFile.getName())) {
            return;
          }
          final PsiElement position = parameters.getPosition();
          List<PsiElement> modules = ResolveImportUtil.resolveModule(
            QualifiedName.fromComponents(PantsUtil.TWITTER, PantsUtil.PANTS),
            psiFile,
            true,
            0
          );
          final CompletionVariantsProcessor processor = new CompletionVariantsProcessor(position);
          for (PsiElement module : modules) {
            module = PyUtil.turnDirIntoInit(module);
            if (module instanceof PyFile) {
              PyResolveUtil.scopeCrawlUp(processor, (PyFile)module, null, null);
            }
          }
          result.addAllElements(processor.getResultList());
        }
      }
    );
  }
}
