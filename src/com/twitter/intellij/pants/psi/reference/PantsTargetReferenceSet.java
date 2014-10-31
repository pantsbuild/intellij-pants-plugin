// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.psi.reference;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.PathUtil;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.generate.tostring.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PantsTargetReferenceSet {
  @NotNull
  private final PyStringLiteralExpression myStringLiteralExpression;
  private List<PsiReference> myReferences;

  public PantsTargetReferenceSet(@NotNull PyStringLiteralExpression element) {
    myStringLiteralExpression = element;
  }

  protected List<PsiReference> getReferences() {
    if (myReferences == null) {
      myReferences = getFileReferences(myStringLiteralExpression);
    }
    return myReferences;
  }

  @NotNull
  private List<PsiReference> getFileReferences(@NotNull PyStringLiteralExpression expression) {
    final VirtualFile pantsWorkingDir = PantsUtil.findPantsWorkingDir(expression.getContainingFile());
    if (pantsWorkingDir == null) {
      return Collections.emptyList();
    }

    final String[] pathAndTarget = expression.getStringValue().split(":");
    String path = PathUtil.toSystemIndependentName(pathAndTarget[0]);
    final String target = pathAndTarget.length > 1 ? pathAndTarget[1] : null;

    final List<PsiReference> result = new ArrayList<PsiReference>();

    assert path != null;
    if (!StringUtil.isEmpty(path) && !path.endsWith("/")) {
      path = path + "/";
    }
    int prevIndex = 0;
    for (int i = 0; i < path.length(); ++i) {
      if (path.charAt(i) != '/') continue;

      final TextRange range = TextRange.create(
        expression.valueOffsetToTextOffset(prevIndex),
        expression.valueOffsetToTextOffset(i)
      );
      result.add(
        new PantsVirtualFileReference(
          myStringLiteralExpression,
          range,
          path.substring(prevIndex, i),
          path.substring(0, i)
        )
      );

      prevIndex = i + 1;
    }

    // todo: add target name ref

    return result;
  }
}
