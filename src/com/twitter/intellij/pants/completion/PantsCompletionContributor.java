package com.twitter.intellij.pants.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.PyKeywordArgument;
import com.jetbrains.python.psi.PyListLiteralExpression;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import com.twitter.intellij.pants.index.PantsTargetIndex;
import com.twitter.intellij.pants.util.PantsUtil;
import org.apache.velocity.texen.util.FileUtil;
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
    extend(
      CompletionType.BASIC,
      psiElement().withParent(PyStringLiteralExpression.class),
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
          final PsiElement position = parameters.getOriginalPosition();
          final PsiElement stringLiteral = position != null ? position.getParent() : null;
          assert stringLiteral instanceof PyStringLiteralExpression;
          final PsiElement parent = stringLiteral.getParent();
          if (!(parent instanceof PyListLiteralExpression)) {
            return;
          }
          final PsiElement parentParent = parent.getParent();
          if (parentParent instanceof PyKeywordArgument && "dependencies".equalsIgnoreCase(((PyKeywordArgument)parentParent).getKeyword())) {
            final int caretPosition = parameters.getOffset() - position.getTextOffset();
            final String text = ((PyStringLiteralExpression)stringLiteral).getStringValue();
            final int index = text.indexOf(':');
            final String path = index > 0 ? text.substring(0, index) : text;
            if (caretPosition > path.length() + 1) {
              return;
            }
            final String pathPrefix = path.substring(0, caretPosition - 1);
            final String relativeDirPath = VfsUtil.getParentDir(pathPrefix);
            final VirtualFile pantsWorkingDir = PantsUtil.findPantsWorkingDir(psiFile.getVirtualFile());
            final VirtualFile dir = pantsWorkingDir != null ? pantsWorkingDir.findFileByRelativePath(StringUtil.notNullize(relativeDirPath)) : null;
            if (dir == null || !dir.isDirectory()) {
              return;
            }
            for (VirtualFile virtualFile : dir.getChildren()) {
              if (!virtualFile.isDirectory()) {
                continue;
              }
              result.addElement(LookupElementBuilder.create(virtualFile.getName()));
            }
          }
        }
      }
    );
  }
}
