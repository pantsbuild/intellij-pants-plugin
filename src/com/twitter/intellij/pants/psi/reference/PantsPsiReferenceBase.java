// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class PantsPsiReferenceBase implements PsiReference {
  private final PsiElement myElement;
  private final TextRange myRange;
  private final String myText;
  private final String myRelativePath;

  public PantsPsiReferenceBase(
    @NotNull PsiElement element,
    @NotNull TextRange range,
    @Nls String text,
    @Nls String relativePath
  ) {
    myElement = element;
    myRange = range;
    myText = text;
    myRelativePath = relativePath;
  }

  @Override
  public PsiElement getElement() {
    return myElement;
  }

  @Override
  public TextRange getRangeInElement() {
    return myRange;
  }

  @Override
  @NotNull
  public String getCanonicalText() {
    return myText;
  }

  public String getText() {
    return myText;
  }

  public String getRelativePath() {
    return myRelativePath;
  }

  @Override
  public boolean isSoft() {
    return false;
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    // todo(@fkorotkov): support it with
    return false;
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    return getElement();
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    return getElement();
  }

  protected Optional<VirtualFile>findFile() {
    return findFile(myRelativePath);
  }

  protected Optional<VirtualFile> findFile(@NotNull String relativePath) {
    return PantsUtil.findFileRelativeToBuildRoot(myElement.getContainingFile(), relativePath);
  }
}
