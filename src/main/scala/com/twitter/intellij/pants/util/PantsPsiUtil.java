// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyArgumentList;
import com.jetbrains.python.psi.PyCallExpression;
import com.jetbrains.python.psi.PyDictLiteralExpression;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyExpressionStatement;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyKeyValueExpression;
import com.jetbrains.python.psi.PyKeywordArgument;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.PyReturnStatement;
import com.jetbrains.python.psi.PyStatement;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.openapi.util.text.StringUtil.unquoteString;

public class PantsPsiUtil {

  @NotNull
  public static Map<String, PyCallExpression> findTargets(@Nullable PsiFile file) {
    if (file == null) {
      return Collections.emptyMap();
    }
    final Map<String, PyCallExpression> result = new HashMap<>();
    for (PyExpressionStatement statement : PsiTreeUtil.findChildrenOfType(file, PyExpressionStatement.class)) {
      final Pair<String, PyCallExpression> nameExpressionPair = findTarget(statement);
      if (nameExpressionPair != null) {
        result.put(nameExpressionPair.getFirst(), nameExpressionPair.getSecond());
      }
    }
    return result;
  }

  @Nullable
  public static Pair<String, PyCallExpression> findTarget(@NotNull PyExpressionStatement statement) {
    final PyCallExpression expression = PsiTreeUtil.findChildOfType(statement, PyCallExpression.class);
    final PyExpression callee = expression != null ? expression.getCallee() : null;
    final PyArgumentList argumentList = expression != null ? expression.getArgumentList() : null;
    final PyKeywordArgument nameArgument = argumentList != null ? argumentList.getKeywordArgument("name") : null;
    final PyExpression valueExpression = nameArgument != null ? nameArgument.getValueExpression() : null;
    if (valueExpression != null && callee != null) {
      return Pair.create(unquoteString(valueExpression.getText()), expression);
    }
    return null;
  }

  @NotNull
  public static Map<String, PyReferenceExpression> findTargetDefinitions(@NotNull PyFile pyFile) {
    final PyFunction buildFileAliases = pyFile.findTopLevelFunction("build_file_aliases");
    final PyStatement[] statements =
      buildFileAliases != null ? buildFileAliases.getStatementList().getStatements() : PyStatement.EMPTY_ARRAY;
    final Map<String, PyReferenceExpression> result = new HashMap<>();
    for (PyStatement statement : statements) {
      if (!(statement instanceof PyReturnStatement)) {
        continue;
      }
      final PyExpression returnExpression = ((PyReturnStatement)statement).getExpression();
      if (!(returnExpression instanceof PyCallExpression)) {
        continue;
      }
      final PyArgumentList argumentList = ((PyCallExpression)returnExpression).getArgumentList();
      final Collection<PyKeywordArgument> targetDefinitions = PsiTreeUtil.findChildrenOfType(argumentList, PyKeywordArgument.class);
      for (PyKeywordArgument targets : targetDefinitions) {
        final PyExpression targetsExpression = targets != null ? targets.getValueExpression() : null;
        if (targetsExpression instanceof PyDictLiteralExpression) {
          for (PyKeyValueExpression keyValueExpression : ((PyDictLiteralExpression)targetsExpression).getElements()) {
            final PyExpression keyExpression = keyValueExpression.getKey();
            final PyExpression valueExpression = keyValueExpression.getValue();
            if (keyExpression instanceof PyStringLiteralExpression) {
              result.put(
                ((PyStringLiteralExpression)keyExpression).getStringValue(),
                valueExpression instanceof PyReferenceExpression ? (PyReferenceExpression)valueExpression : null
              );
            }
          }
        }
      }
    }
    return result;
  }
}