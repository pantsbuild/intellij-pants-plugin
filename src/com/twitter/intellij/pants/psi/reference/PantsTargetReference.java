// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.reference;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.python.FunctionParameter;
import com.jetbrains.python.nameResolver.FQNamesProvider;
import com.jetbrains.python.psi.PyArgumentList;
import com.jetbrains.python.psi.PyCallExpression;
import com.jetbrains.python.psi.PyCallable;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.resolve.PyResolveContext;
import com.jetbrains.python.psi.types.PyType;
import com.jetbrains.python.psi.types.TypeEvalContext;
import com.twitter.intellij.pants.util.PantsPsiUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PantsTargetReference extends PantsPsiReferenceBase {
  public PantsTargetReference(@NotNull PsiElement element, @NotNull TextRange range, @Nls String text, @Nls String relativePath) {
    super(element, range, text, relativePath);
  }

  @Nullable
  private PsiFile findBuildFile() {
    if (StringUtil.isEmpty(getRelativePath())) {
      // same file reference
      return getElement().getContainingFile();
    }
    final VirtualFile buildFile = PantsUtil.findBUILDFile(findFile());
    if (buildFile == null) {
      return null;
    }
    final PsiManager psiManager = PsiManager.getInstance(getElement().getProject());
    return psiManager.findFile(buildFile);
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    return ContainerUtil.map2Array(
      PantsPsiUtil.findTargets(findBuildFile()).keySet(),
      new Function<String, Object>() {
        @Override
        public Object fun(String targetName) {
          return LookupElementBuilder.create(targetName);
        }
      }
    );
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    // Sanitize ':' out of the target name
    String sanitizedText = getText().replace(":", "");
    PsiElement retVal = PantsPsiUtil.findTargets(findBuildFile()).get(sanitizedText);
    if (retVal != null) {
      return retVal;
    }
    return new PyCallExpression() {
      @Nullable
      @Override
      public PyExpression getCallee() {
        return null;
      }

      @Nullable
      @Override
      public PyArgumentList getArgumentList() {
        return null;
      }

      @NotNull
      @Override
      public PyExpression[] getArguments() {
        return new PyExpression[0];
      }

      @Nullable
      @Override
      public <T extends PsiElement> T getArgument(int index, Class<T> argClass) {
        return null;
      }

      @Nullable
      @Override
      public <T extends PsiElement> T getArgument(int index, String keyword, Class<T> argClass) {
        return null;
      }

      @Nullable
      @Override
      public <T extends PsiElement> T getArgument(@NotNull FunctionParameter parameter, @NotNull Class<T> argClass) {
        return null;
      }

      @Nullable
      @Override
      public PyExpression getKeywordArgument(String keyword) {
        return null;
      }

      @Override
      public void addArgument(PyExpression expression) {

      }

      @Nullable
      @Override
      public PyMarkedCallee resolveCallee(PyResolveContext resolveContext) {
        return null;
      }

      @Nullable
      @Override
      public PyCallable resolveCalleeFunction(PyResolveContext resolveContext) {
        return null;
      }

      @Nullable
      @Override
      public PyMarkedCallee resolveCallee(PyResolveContext resolveContext, int implicitOffset) {
        return null;
      }

      @Override
      public boolean isCalleeText(@NotNull String... nameCandidates) {
        return false;
      }

      @Override
      public boolean isCallee(@NotNull FQNamesProvider... name) {
        return false;
      }

      @Nullable
      @Override
      public PyType getType(
        @NotNull TypeEvalContext context, @NotNull TypeEvalContext.Key key
      ) {
        return null;
      }

      @Nullable
      @Override
      public String getName() {
        return null;
      }

      @Nullable
      @Override
      public ItemPresentation getPresentation() {
        return null;
      }

      @Override
      public void navigate(boolean requestFocus) {

      }

      @Override
      public boolean canNavigate() {
        return false;
      }

      @Override
      public boolean canNavigateToSource() {
        return false;
      }

      @NotNull
      @Override
      public Project getProject() throws PsiInvalidElementAccessException {
        return null;
      }

      @NotNull
      @Override
      public Language getLanguage() {
        return null;
      }

      @Override
      public PsiManager getManager() {
        return null;
      }

      @NotNull
      @Override
      public PsiElement[] getChildren() {
        return new PsiElement[0];
      }

      @Override
      public PsiElement getParent() {
        return null;
      }

      @Override
      public PsiElement getFirstChild() {
        return null;
      }

      @Override
      public PsiElement getLastChild() {
        return null;
      }

      @Override
      public PsiElement getNextSibling() {
        return null;
      }

      @Override
      public PsiElement getPrevSibling() {
        return null;
      }

      @Override
      public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
        return null;
      }

      @Override
      public TextRange getTextRange() {
        return null;
      }

      @Override
      public int getStartOffsetInParent() {
        return 0;
      }

      @Override
      public int getTextLength() {
        return 0;
      }

      @Nullable
      @Override
      public PsiElement findElementAt(int offset) {
        return null;
      }

      @Nullable
      @Override
      public PsiReference findReferenceAt(int offset) {
        return null;
      }

      @Override
      public int getTextOffset() {
        return 0;
      }

      @Override
      public String getText() {
        return null;
      }

      @NotNull
      @Override
      public char[] textToCharArray() {
        return new char[0];
      }

      @Override
      public PsiElement getNavigationElement() {
        return null;
      }

      @Override
      public PsiElement getOriginalElement() {
        return null;
      }

      @Override
      public boolean textMatches(@NotNull CharSequence text) {
        return false;
      }

      @Override
      public boolean textMatches(@NotNull PsiElement element) {
        return false;
      }

      @Override
      public boolean textContains(char c) {
        return false;
      }

      @Override
      public void accept(@NotNull PsiElementVisitor visitor) {

      }

      @Override
      public void acceptChildren(@NotNull PsiElementVisitor visitor) {

      }

      @Override
      public PsiElement copy() {
        return null;
      }

      @Override
      public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
        return null;
      }

      @Override
      public PsiElement addBefore(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
        return null;
      }

      @Override
      public PsiElement addAfter(@NotNull PsiElement element, @Nullable PsiElement anchor) throws IncorrectOperationException {
        return null;
      }

      @Override
      public void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {

      }

      @Override
      public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        return null;
      }

      @Override
      public PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor)
        throws IncorrectOperationException {
        return null;
      }

      @Override
      public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        return null;
      }

      @Override
      public void delete() throws IncorrectOperationException {

      }

      @Override
      public void checkDelete() throws IncorrectOperationException {

      }

      @Override
      public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {

      }

      @Override
      public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        return null;
      }

      @Override
      public boolean isValid() {
        return false;
      }

      @Override
      public boolean isWritable() {
        return false;
      }

      @Nullable
      @Override
      public PsiReference getReference() {
        return null;
      }

      @NotNull
      @Override
      public PsiReference[] getReferences() {
        return new PsiReference[0];
      }

      @Nullable
      @Override
      public <T> T getCopyableUserData(Key<T> key) {
        return null;
      }

      @Override
      public <T> void putCopyableUserData(Key<T> key, @Nullable T value) {

      }

      @Override
      public boolean processDeclarations(
        @NotNull PsiScopeProcessor processor, @NotNull ResolveState state, @Nullable PsiElement lastParent, @NotNull PsiElement place
      ) {
        return false;
      }

      @Nullable
      @Override
      public PsiElement getContext() {
        return null;
      }

      @Override
      public boolean isPhysical() {
        return false;
      }

      @NotNull
      @Override
      public GlobalSearchScope getResolveScope() {
        return null;
      }

      @NotNull
      @Override
      public SearchScope getUseScope() {
        return null;
      }

      @Override
      public ASTNode getNode() {
        return null;
      }

      @Override
      public boolean isEquivalentTo(PsiElement another) {
        return false;
      }

      @Override
      public Icon getIcon(int flags) {
        return null;
      }

      @Nullable
      @Override
      public <T> T getUserData(@NotNull Key<T> key) {
        return null;
      }

      @Override
      public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {

      }
    };
  }
}
