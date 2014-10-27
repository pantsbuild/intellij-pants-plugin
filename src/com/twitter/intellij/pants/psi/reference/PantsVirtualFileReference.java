package com.twitter.intellij.pants.psi.reference;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PantsVirtualFileReference implements PsiReference {
  private final PsiElement myElement;
  private final TextRange myRange;
  private final String myText;
  private final String myRelativePath;

  public PantsVirtualFileReference(
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

  @Nullable
  private VirtualFile findFile() {
    return findFile(myRelativePath);
  }

  @Nullable
  private VirtualFile findFile(@NotNull String relativePath) {
    final VirtualFile workingDir = PantsUtil.findPantsWorkingDir(myElement.getContainingFile());
    return workingDir != null ? workingDir.findFileByRelativePath(relativePath) : null;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    final PsiManager psiManager = PsiManager.getInstance(myElement.getProject());
    final VirtualFile parent = findFile(PathUtil.getParentPath(myRelativePath));
    final List<Object> variants = ContainerUtil.map(
      ContainerUtil.filter(
        parent != null ? parent.getChildren() : VirtualFile.EMPTY_ARRAY,
        new Condition<VirtualFile>() {
          @Override
          public boolean value(VirtualFile file) {
            return file.isDirectory();
          }
        }
      ),
      new Function<VirtualFile, Object>() {
        @Override
        public Object fun(VirtualFile file) {
          final PsiFile psiFile = psiManager.findFile(file);
          if (psiFile != null) {
            return LookupElementBuilder.create(psiFile);
          }
          else {
            return LookupElementBuilder.create(file.getPresentableName());
          }
        }
      }
    );
    return ArrayUtil.toObjectArray(variants);
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    final VirtualFile virtualFile = findFile();
    if (virtualFile == null) {
      return null;
    }
    final PsiManager psiManager = PsiManager.getInstance(myElement.getProject());
    final PsiFile file = psiManager.findFile(virtualFile);
    return file != null ? file : psiManager.findDirectory(virtualFile);
  }
}
