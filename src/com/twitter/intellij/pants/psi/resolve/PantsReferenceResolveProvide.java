package com.twitter.intellij.pants.psi.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.QualifiedName;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyQualifiedExpression;
import com.jetbrains.python.psi.PyUtil;
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider;
import com.jetbrains.python.psi.resolve.RatedResolveResult;
import com.jetbrains.python.psi.resolve.ResolveImportUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PantsReferenceResolveProvide implements PyReferenceResolveProvider {
  @NotNull
  @Override
  public List<RatedResolveResult> resolveName(@NotNull PyQualifiedExpression element, @NotNull List<PsiElement> definers) {
    PsiFile containingFile = element.getContainingFile();
    return PantsUtil.BUILD.equals(containingFile.getName()) ?
           resolvePantsName(element) :
           Collections.<RatedResolveResult>emptyList();
  }

  private List<RatedResolveResult> resolvePantsName(@NotNull PyQualifiedExpression element) {
    String name = element.getName();

    List<PsiElement> modules = ResolveImportUtil.resolveModule(
      QualifiedName.fromComponents(PantsUtil.TWITTER, PantsUtil.PANTS),
      element.getContainingFile(),
      true,
      0
    );
    final List<RatedResolveResult> result = new ArrayList<RatedResolveResult>();
    for (PsiElement module : modules) {
      module = PyUtil.turnDirIntoInit(module);
      if (module instanceof PyFile) {
        final PsiElement target = ((PyFile)module).getElementNamed(name);
        if (target != null) {
          result.add(new RatedResolveResult(RatedResolveResult.RATE_NORMAL, target));
        }
      }
    }
    return result;
  }
}
