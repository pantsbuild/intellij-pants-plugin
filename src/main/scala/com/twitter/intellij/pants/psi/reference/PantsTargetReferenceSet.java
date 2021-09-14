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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;

/**
 * Converts a python string literal in a build file into PantsPsiReferences
 */
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
    final Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(expression.getContainingFile());
    if (!buildRoot.isPresent()) {
      return Collections.emptyList();
    }

    final PartialTargetAddress address = PartialTargetAddress.parse(expression.getStringValue());
    final List<PsiReference> result = new ArrayList<>();

    result.addAll(createPathSegments(expression, address.normalizedPath));

    if (address.explicitTarget != null) {
      result.add(createTargetSegmentReference(expression, address));
    }
    return result;
  }

  @NotNull
  private List<PsiReference> createPathSegments(@NotNull PyStringLiteralExpression expression, @NotNull String path) {
    final List<PsiReference> result = new ArrayList<>();
    int prevIndex = 0;
    for (int i = 0; i < path.length(); ++i) {
      if (path.charAt(i) != '/') continue;

      result.add(
        createPathSegmentReference(expression, path, prevIndex, i)
      );
      prevIndex = i + 1;
    }

    return result;
  }

  @NotNull
  private PantsVirtualFileReference createPathSegmentReference(
    @NotNull PyStringLiteralExpression expression,
    @NotNull String path,
    int prevIndex,
    int endIndex
  ) {
    final TextRange range = TextRange.create(
      expression.valueOffsetToTextOffset(prevIndex),
      expression.valueOffsetToTextOffset(endIndex)
    );
    return new PantsVirtualFileReference(
      myStringLiteralExpression,
      range,
      path.substring(prevIndex, endIndex),
      path.substring(0, endIndex)
    );
  }

  @NotNull
  private PantsTargetReference createTargetSegmentReference(
    @NotNull PyStringLiteralExpression expression,
    @NotNull PartialTargetAddress address
  ) {
    final TextRange range = TextRange.create(
      expression.valueOffsetToTextOffset(address.startOfExplicitTarget()),
      expression.valueOffsetToTextOffset(address.valueLength)
    );
    return new PantsTargetReference(
      myStringLiteralExpression,
      range, address.explicitTarget, address.normalizedPath
    );
  }

  public static class PartialTargetAddress {
    @Nullable
    private final String explicitTarget;
    @NotNull
    private final String normalizedPath;
    private final int valueLength;
    private final int colonIndex;

    @Nullable
    public String getExplicitTarget() {
      return explicitTarget;
    }

    @NotNull
    public String getNormalizedPath() {
      return normalizedPath;
    }

    public PartialTargetAddress(@Nullable String explicitTarget, @NotNull String normalizedPath, int valueLength, int colonIndex) {
      this.explicitTarget = explicitTarget;
      this.normalizedPath = normalizedPath;
      this.valueLength = valueLength;
      this.colonIndex = colonIndex;
    }

    @NotNull
    public static PartialTargetAddress parse(String value) {
      final int colonIndex = value.indexOf(':');
      final int valueLength = value.length();

      //substringAfter may return empty string if colon is the last character, so null is need in this case
      final String explicitTarget = StringUtil.nullize(StringUtil.substringAfter(value, ":"));
      //substringBefore may return null if colon does not exist, so rawPath is value in this case
      final String rawPath = ObjectUtils.notNull(StringUtil.substringBefore(value, ":"), value);

      String normalizedPath;
      if (rawPath.isEmpty()) {
        normalizedPath = rawPath;
      }
      else {
        final String normalized = PathUtil.toSystemIndependentName(rawPath);
        normalizedPath = normalized.charAt(normalized.length() - 1) == '/' ? normalized : normalized + "/";
      }

      return new PartialTargetAddress(explicitTarget, normalizedPath, valueLength, colonIndex);
    }

    int startOfExplicitTarget() {
      return colonIndex + 1;
    }
  }
}
